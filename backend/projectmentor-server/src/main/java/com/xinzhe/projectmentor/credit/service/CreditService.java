package com.xinzhe.projectmentor.credit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xinzhe.projectmentor.admin.service.AdminService;
import com.xinzhe.projectmentor.auth.entity.User;
import com.xinzhe.projectmentor.auth.interceptor.UserContext;
import com.xinzhe.projectmentor.auth.mapper.UserMapper;
import com.xinzhe.projectmentor.common.BusinessException;
import com.xinzhe.projectmentor.common.ErrorCode;
import com.xinzhe.projectmentor.credit.dto.AdminGrantCreditRequest;
import com.xinzhe.projectmentor.credit.dto.AddCreditRequest;
import com.xinzhe.projectmentor.credit.entity.CreditLog;
import com.xinzhe.projectmentor.credit.entity.UserPlan;
import com.xinzhe.projectmentor.credit.mapper.CreditLogMapper;
import com.xinzhe.projectmentor.credit.mapper.UserPlanMapper;
import com.xinzhe.projectmentor.credit.vo.AdminCreditGrantResultVO;
import com.xinzhe.projectmentor.credit.vo.AdminCreditTransactionVO;
import com.xinzhe.projectmentor.credit.vo.AdminCreditUserDetailVO;
import com.xinzhe.projectmentor.credit.vo.AdminCreditUserVO;
import com.xinzhe.projectmentor.credit.vo.CreditInfoVO;
import com.xinzhe.projectmentor.credit.vo.CreditLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CreditService {

    private final UserPlanMapper userPlanMapper;

    private final CreditLogMapper creditLogMapper;

    private final UserMapper userMapper;

    private final AdminService adminService;

    public CreditInfoVO getMyCredits() {
        Long userId = getCurrentUserId();
        UserPlan userPlan = getOrCreateUserPlan(userId);

        return CreditInfoVO.builder()
                .userId(userId)
                .planType(userPlan.getPlanType())
                .remainingCredits(userPlan.getRemainingCredits())
                .expireTime(userPlan.getExpireTime())
                .build();
    }

    public List<CreditLogVO> listMyCreditLogs() {
        Long userId = getCurrentUserId();

        List<CreditLog> logs = creditLogMapper.selectList(
                new LambdaQueryWrapper<CreditLog>()
                        .eq(CreditLog::getUserId, userId)
                        .orderByDesc(CreditLog::getCreateTime)
        );

        return logs.stream()
                .map(this::toVO)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public CreditInfoVO addCreditsByAdmin(AddCreditRequest request) {
        AdminGrantCreditRequest grantRequest = new AdminGrantCreditRequest();
        grantRequest.setUserId(request.getUserId());
        grantRequest.setAmount(request.getAmount());
        grantRequest.setReason(StringUtils.hasText(request.getRemark()) ? request.getRemark() : "管理员手动增加额度");

        AdminCreditGrantResultVO result = grantCreditsByAdmin(grantRequest);

        UserPlan userPlan = getOrCreateUserPlan(request.getUserId());
        return CreditInfoVO.builder()
                .userId(result.getUserId())
                .planType(userPlan.getPlanType())
                .remainingCredits(result.getNewBalance())
                .expireTime(userPlan.getExpireTime())
                .build();
    }

    public List<AdminCreditUserVO> searchCreditUsers(String keyword, Integer limit) {
        adminService.requireAdmin();

        List<User> users = userMapper.selectList(buildUserSearchWrapper(keyword, limit));
        Map<Long, Integer> balanceMap = loadBalanceMap(users.stream()
                .map(User::getId)
                .collect(Collectors.toSet()));

        return users.stream()
                .map(user -> AdminCreditUserVO.builder()
                        .userId(user.getId())
                        .email(user.getEmail())
                        .nickname(user.getUsername())
                        .creditBalance(balanceMap.getOrDefault(user.getId(), 0))
                        .createTime(user.getCreateTime())
                        .build())
                .toList();
    }

    public AdminCreditUserDetailVO getAdminCreditUserDetail(Long userId) {
        adminService.requireAdmin();
        User user = requireExistingUser(userId);
        UserPlan userPlan = getOrCreateUserPlan(userId);

        List<AdminCreditTransactionVO> transactions = creditLogMapper.selectList(
                        new LambdaQueryWrapper<CreditLog>()
                                .eq(CreditLog::getUserId, userId)
                                .orderByDesc(CreditLog::getCreateTime)
                                .last("LIMIT 10")
                )
                .stream()
                .map(this::toAdminTransactionVO)
                .toList();

        return AdminCreditUserDetailVO.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getUsername())
                .creditBalance(userPlan.getRemainingCredits() == null ? 0 : userPlan.getRemainingCredits())
                .recentTransactions(transactions)
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public AdminCreditGrantResultVO grantCreditsByAdmin(AdminGrantCreditRequest request) {
        User adminUser = adminService.requireAdminUser();

        if (request.getAmount() == null || request.getAmount() <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "发放额度必须为正整数");
        }
        if (request.getAmount() > 10000) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "单次发放额度不能超过 10000");
        }
        if (!StringUtils.hasText(request.getReason()) || request.getReason().trim().length() < 2) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "发放原因不能为空且至少 2 个字符");
        }

        User targetUser = requireExistingUser(request.getUserId());

        String reason = request.getReason().trim();
        if (reason.length() > 200) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "发放原因不能超过 200 个字符");
        }

        UserPlan userPlan = getOrCreateUserPlan(request.getUserId());

        int before = userPlan.getRemainingCredits() == null ? 0 : userPlan.getRemainingCredits();
        int after = before + request.getAmount();

        userPlan.setRemainingCredits(after);
        userPlanMapper.updateById(userPlan);

        CreditLog creditLog = createCreditLog(
                request.getUserId(),
                request.getAmount(),
                before,
                after,
                "ADMIN_GRANT",
                null,
                buildAdminGrantRemark(reason, adminUser)
        );

        return AdminCreditGrantResultVO.builder()
                .userId(request.getUserId())
                .email(targetUser.getEmail())
                .grantedAmount(request.getAmount())
                .newBalance(after)
                .transactionId(creditLog.getId())
                .build();
    }

    /**
     * 消耗额度。
     *
     * 【重点理解】
     * 这里使用 REQUIRES_NEW，表示额度扣减会开启一个独立事务。
     * 这样后续报告生成失败时，我们才能再用独立事务返还额度。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void consumeCredits(Long userId,
                               int cost,
                               String operationType,
                               Long businessId,
                               String remark) {
        if (cost <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "消耗额度必须大于 0");
        }

        UserPlan userPlan = getOrCreateUserPlan(userId);

        int before = userPlan.getRemainingCredits() == null ? 0 : userPlan.getRemainingCredits();

        if (before < cost) {
            throw new BusinessException(
                    ErrorCode.CREDIT_NOT_ENOUGH,
                    "额度不足，无法调用 AI。请减少 AI 操作或联系管理员发放额度。当前剩余额度：" + before
            );
        }

        int after = before - cost;

        userPlan.setRemainingCredits(after);
        userPlanMapper.updateById(userPlan);

        createCreditLog(
                userId,
                -cost,
                before,
                after,
                operationType,
                businessId,
                remark
        );
    }

    /**
     * 返还额度。
     *
     * 【重点理解】
     * 如果报告生成失败，不能让用户白白损失额度。
     * 所以失败时需要返还，并记录 CREDIT_REFUND 流水。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void refundCredits(Long userId,
                              int amount,
                              String operationType,
                              Long businessId,
                              String remark) {
        if (amount <= 0) {
            return;
        }

        UserPlan userPlan = getOrCreateUserPlan(userId);

        int before = userPlan.getRemainingCredits() == null ? 0 : userPlan.getRemainingCredits();
        int after = before + amount;

        userPlan.setRemainingCredits(after);
        userPlanMapper.updateById(userPlan);

        createCreditLog(
                userId,
                amount,
                before,
                after,
                operationType,
                businessId,
                remark
        );
    }

    private UserPlan getOrCreateUserPlan(Long userId) {
        UserPlan userPlan = userPlanMapper.selectOne(
                new LambdaQueryWrapper<UserPlan>()
                        .eq(UserPlan::getUserId, userId)
                        .last("LIMIT 1")
        );

        if (userPlan != null) {
            return userPlan;
        }

        UserPlan newPlan = new UserPlan();
        newPlan.setUserId(userId);
        newPlan.setPlanType("FREE");
        newPlan.setRemainingCredits(0);

        userPlanMapper.insert(newPlan);

        return newPlan;
    }

    private CreditLog createCreditLog(Long userId,
                                      Integer changeAmount,
                                      Integer beforeAmount,
                                      Integer afterAmount,
                                      String operationType,
                                      Long businessId,
                                      String remark) {
        CreditLog creditLog = new CreditLog();
        creditLog.setUserId(userId);
        creditLog.setChangeAmount(changeAmount);
        creditLog.setBeforeAmount(beforeAmount);
        creditLog.setAfterAmount(afterAmount);
        creditLog.setOperationType(operationType);
        creditLog.setBusinessId(businessId);
        creditLog.setRemark(remark);

        creditLogMapper.insert(creditLog);
        return creditLog;
    }

    private LambdaQueryWrapper<User> buildUserSearchWrapper(String keyword, Integer limit) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .select(User::getId, User::getEmail, User::getUsername, User::getCreateTime)
                .orderByDesc(User::getCreateTime)
                .last("LIMIT " + sanitizeLimit(limit));

        if (!StringUtils.hasText(keyword)) {
            return wrapper;
        }

        String normalizedKeyword = keyword.trim();
        Long userId = parseLong(normalizedKeyword);

        wrapper.and(query -> {
            query.like(User::getEmail, normalizedKeyword)
                    .or()
                    .like(User::getUsername, normalizedKeyword);
            if (userId != null) {
                query.or().eq(User::getId, userId);
            }
        });

        return wrapper;
    }

    private Map<Long, Integer> loadBalanceMap(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return userPlanMapper.selectList(new LambdaQueryWrapper<UserPlan>()
                        .select(UserPlan::getUserId, UserPlan::getRemainingCredits)
                        .in(UserPlan::getUserId, userIds))
                .stream()
                .collect(Collectors.toMap(
                        UserPlan::getUserId,
                        plan -> plan.getRemainingCredits() == null ? 0 : plan.getRemainingCredits(),
                        (left, right) -> left,
                        HashMap::new
                ));
    }

    private int sanitizeLimit(Integer limit) {
        int safeLimit = limit == null ? 10 : limit;
        return Math.max(1, Math.min(safeLimit, 20));
    }

    private Long parseLong(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private User requireExistingUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户ID不能为空");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "目标用户不存在");
        }
        return user;
    }

    private String buildAdminGrantRemark(String reason, User adminUser) {
        return "管理员发放：" + reason
                + "；adminId=" + adminUser.getId()
                + "；adminEmail=" + adminUser.getEmail();
    }

    private AdminCreditTransactionVO toAdminTransactionVO(CreditLog log) {
        return AdminCreditTransactionVO.builder()
                .id(log.getId())
                .changeAmount(log.getChangeAmount())
                .type(log.getOperationType())
                .reason(log.getRemark())
                .createTime(log.getCreateTime())
                .build();
    }

    private Long getCurrentUserId() {
        Long userId = UserContext.getUserId();

        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return userId;
    }

    private CreditLogVO toVO(CreditLog log) {
        return CreditLogVO.builder()
                .id(log.getId())
                .userId(log.getUserId())
                .changeAmount(log.getChangeAmount())
                .beforeAmount(log.getBeforeAmount())
                .afterAmount(log.getAfterAmount())
                .operationType(log.getOperationType())
                .businessId(log.getBusinessId())
                .remark(log.getRemark())
                .createTime(log.getCreateTime())
                .build();
    }
}
