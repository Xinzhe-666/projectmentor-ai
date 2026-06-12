package com.xinzhe.projectmentor.credit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xinzhe.projectmentor.admin.service.AdminService;
import com.xinzhe.projectmentor.auth.entity.User;
import com.xinzhe.projectmentor.auth.interceptor.UserContext;
import com.xinzhe.projectmentor.auth.mapper.UserMapper;
import com.xinzhe.projectmentor.common.BusinessException;
import com.xinzhe.projectmentor.common.ErrorCode;
import com.xinzhe.projectmentor.common.PageResult;
import com.xinzhe.projectmentor.credit.CreditCostConstants;
import com.xinzhe.projectmentor.credit.dto.AddCreditRequest;
import com.xinzhe.projectmentor.credit.dto.AdminCreditAdjustmentRequest;
import com.xinzhe.projectmentor.credit.dto.AdminGrantCreditRequest;
import com.xinzhe.projectmentor.credit.entity.CreditLog;
import com.xinzhe.projectmentor.credit.entity.UserPlan;
import com.xinzhe.projectmentor.credit.mapper.AdminCreditQueryMapper;
import com.xinzhe.projectmentor.credit.mapper.CreditLogMapper;
import com.xinzhe.projectmentor.credit.mapper.UserPlanMapper;
import com.xinzhe.projectmentor.credit.vo.AdminCreditAdjustmentResultVO;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CreditService {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private static final int MAX_PAGE_SIZE = 100;

    private static final int MAX_ADMIN_ADJUSTMENT = 10000;

    private final UserPlanMapper userPlanMapper;

    private final CreditLogMapper creditLogMapper;

    private final UserMapper userMapper;

    private final AdminService adminService;

    private final AdminCreditQueryMapper adminCreditQueryMapper;

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

        return creditLogMapper.selectList(
                        new LambdaQueryWrapper<CreditLog>()
                                .eq(CreditLog::getUserId, userId)
                                .orderByDesc(CreditLog::getCreateTime)
                )
                .stream()
                .map(this::toVO)
                .toList();
    }

    public PageResult<AdminCreditUserVO> searchCreditUsers(String keyword,
                                                            Integer page,
                                                            Integer size,
                                                            String sort) {
        adminService.requireAdmin();

        int safePage = sanitizePage(page);
        int safeSize = sanitizeSize(size);
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        CreditUserSort creditUserSort = parseSort(sort);
        int offset = (safePage - 1) * safeSize;

        return PageResult.<AdminCreditUserVO>builder()
                .records(adminCreditQueryMapper.selectCreditUsers(
                        normalizedKeyword,
                        offset,
                        safeSize,
                        creditUserSort.column(),
                        creditUserSort.direction()
                ))
                .total(adminCreditQueryMapper.countCreditUsers(normalizedKeyword))
                .page(safePage)
                .size(safeSize)
                .build();
    }

    public AdminCreditUserDetailVO getAdminCreditUserDetail(Long userId) {
        adminService.requireAdmin();
        User user = requireExistingUser(userId);
        UserPlan userPlan = getOrCreateUserPlan(userId);
        PageResult<AdminCreditTransactionVO> logs = listAdminCreditLogs(
                userId, 1, 10, null, null, null, null
        );

        return AdminCreditUserDetailVO.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getUsername())
                .creditBalance(currentBalance(userPlan))
                .recentTransactions(logs.getRecords())
                .build();
    }

    public PageResult<AdminCreditTransactionVO> listAdminCreditLogs(Long userId,
                                                                    Integer page,
                                                                    Integer size,
                                                                    String type,
                                                                    String module,
                                                                    LocalDateTime startTime,
                                                                    LocalDateTime endTime) {
        adminService.requireAdmin();
        requireExistingUser(userId);

        int safePage = sanitizePage(page);
        int safeSize = sanitizeSize(size);
        int offset = (safePage - 1) * safeSize;

        LambdaQueryWrapper<CreditLog> countWrapper = buildAdminLogWrapper(
                userId, type, module, startTime, endTime
        );
        long total = creditLogMapper.selectCount(countWrapper);

        LambdaQueryWrapper<CreditLog> listWrapper = buildAdminLogWrapper(
                userId, type, module, startTime, endTime
        );
        listWrapper.orderByDesc(CreditLog::getCreateTime)
                .orderByDesc(CreditLog::getId)
                .last("LIMIT " + offset + ", " + safeSize);

        List<AdminCreditTransactionVO> records = creditLogMapper.selectList(listWrapper)
                .stream()
                .map(this::toAdminTransactionVO)
                .toList();

        return PageResult.<AdminCreditTransactionVO>builder()
                .records(records)
                .total(total)
                .page(safePage)
                .size(safeSize)
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public AdminCreditAdjustmentResultVO grantCreditsByAdmin(Long userId,
                                                              AdminCreditAdjustmentRequest request) {
        User adminUser = adminService.requireAdminUser();
        validateAdjustmentRequest(request);
        User targetUser = requireExistingUser(userId);
        UserPlan userPlan = getOrCreateUserPlan(userId);

        int before = currentBalance(userPlan);
        if (before > Integer.MAX_VALUE - request.getAmount()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "额度余额已达到系统上限");
        }
        int after = before + request.getAmount();

        updateBalance(userPlan, after);
        CreditLog creditLog = createCreditLog(
                userId,
                request.getAmount(),
                before,
                after,
                CreditCostConstants.OP_ADMIN_GRANT,
                null,
                buildAdminRemark("管理员发放", request.getReason(), adminUser)
        );

        return buildAdjustmentResult(
                targetUser,
                request.getAmount(),
                request.getAmount(),
                after,
                CreditCostConstants.OP_ADMIN_GRANT,
                creditLog.getId()
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public AdminCreditAdjustmentResultVO deductCreditsByAdmin(Long userId,
                                                               AdminCreditAdjustmentRequest request) {
        User adminUser = adminService.requireAdminUser();
        validateAdjustmentRequest(request);
        User targetUser = requireExistingUser(userId);
        UserPlan userPlan = getOrCreateUserPlan(userId);

        int before = currentBalance(userPlan);
        if (before < request.getAmount()) {
            throw new BusinessException(
                    ErrorCode.CREDIT_NOT_ENOUGH,
                    "扣除额度不能超过当前余额，当前余额：" + before
            );
        }
        int after = before - request.getAmount();

        updateBalance(userPlan, after);
        CreditLog creditLog = createCreditLog(
                userId,
                -request.getAmount(),
                before,
                after,
                CreditCostConstants.OP_ADMIN_DEDUCT,
                null,
                buildAdminRemark("管理员扣除", request.getReason(), adminUser)
        );

        return buildAdjustmentResult(
                targetUser,
                request.getAmount(),
                -request.getAmount(),
                after,
                CreditCostConstants.OP_ADMIN_DEDUCT,
                creditLog.getId()
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public AdminCreditGrantResultVO grantCreditsByAdmin(AdminGrantCreditRequest request) {
        AdminCreditAdjustmentRequest adjustmentRequest = new AdminCreditAdjustmentRequest();
        adjustmentRequest.setAmount(request.getAmount());
        adjustmentRequest.setReason(request.getReason());

        AdminCreditAdjustmentResultVO result = grantCreditsByAdmin(request.getUserId(), adjustmentRequest);
        return AdminCreditGrantResultVO.builder()
                .userId(result.getUserId())
                .email(result.getEmail())
                .grantedAmount(result.getAdjustedAmount())
                .newBalance(result.getNewBalance())
                .transactionId(result.getTransactionId())
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public CreditInfoVO addCreditsByAdmin(AddCreditRequest request) {
        AdminCreditAdjustmentRequest adjustmentRequest = new AdminCreditAdjustmentRequest();
        adjustmentRequest.setAmount(request.getAmount());
        adjustmentRequest.setReason(StringUtils.hasText(request.getRemark())
                ? request.getRemark()
                : "管理员手动增加额度");

        AdminCreditAdjustmentResultVO result = grantCreditsByAdmin(request.getUserId(), adjustmentRequest);
        UserPlan userPlan = getOrCreateUserPlan(request.getUserId());
        return CreditInfoVO.builder()
                .userId(result.getUserId())
                .planType(userPlan.getPlanType())
                .remainingCredits(result.getNewBalance())
                .expireTime(userPlan.getExpireTime())
                .build();
    }

    /**
     * 消耗额度使用独立事务，便于后续失败时通过另一个独立事务返还额度。
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
        int before = currentBalance(userPlan);

        if (before < cost) {
            throw new BusinessException(
                    ErrorCode.CREDIT_NOT_ENOUGH,
                    "额度不足，无法调用 AI。请减少 AI 操作或联系管理员发放额度。当前剩余额度：" + before
            );
        }

        int after = before - cost;
        updateBalance(userPlan, after);
        createCreditLog(userId, -cost, before, after, operationType, businessId, remark);
    }

    /**
     * AI 调用或结果保存失败时返还额度，并保留可审计流水。
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
        int before = currentBalance(userPlan);
        int after = before + amount;

        updateBalance(userPlan, after);
        createCreditLog(userId, amount, before, after, operationType, businessId, remark);
    }

    private LambdaQueryWrapper<CreditLog> buildAdminLogWrapper(Long userId,
                                                               String type,
                                                               String module,
                                                               LocalDateTime startTime,
                                                               LocalDateTime endTime) {
        LambdaQueryWrapper<CreditLog> wrapper = new LambdaQueryWrapper<CreditLog>()
                .eq(CreditLog::getUserId, userId);

        if (StringUtils.hasText(module)) {
            wrapper.eq(CreditLog::getOperationType, module.trim());
        }

        if (StringUtils.hasText(type)) {
            String normalizedType = type.trim().toUpperCase(Locale.ROOT);
            switch (normalizedType) {
                case "REFUND" -> wrapper.like(CreditLog::getOperationType, "REFUND");
                case "CONSUME" -> wrapper.lt(CreditLog::getChangeAmount, 0)
                        .notLike(CreditLog::getOperationType, "REFUND");
                case "GRANT" -> wrapper.gt(CreditLog::getChangeAmount, 0)
                        .notLike(CreditLog::getOperationType, "REFUND");
                default -> wrapper.eq(CreditLog::getOperationType, normalizedType);
            }
        }

        if (startTime != null) {
            wrapper.ge(CreditLog::getCreateTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(CreditLog::getCreateTime, endTime);
        }
        return wrapper;
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

    private void updateBalance(UserPlan userPlan, int balance) {
        userPlan.setRemainingCredits(balance);
        if (userPlanMapper.updateById(userPlan) != 1) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "额度余额更新失败");
        }
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

        if (creditLogMapper.insert(creditLog) != 1) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "额度流水写入失败");
        }
        return creditLog;
    }

    private void validateAdjustmentRequest(AdminCreditAdjustmentRequest request) {
        if (request == null || request.getAmount() == null || request.getAmount() <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "调整额度必须为正整数");
        }
        if (request.getAmount() > MAX_ADMIN_ADJUSTMENT) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "单次调整额度不能超过 10000");
        }
        if (!StringUtils.hasText(request.getReason()) || request.getReason().trim().length() < 2) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "调整原因不能为空且至少 2 个字符");
        }
        if (request.getReason().trim().length() > 200) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "调整原因不能超过 200 个字符");
        }
    }

    private AdminCreditAdjustmentResultVO buildAdjustmentResult(User targetUser,
                                                                 int adjustedAmount,
                                                                 int changeAmount,
                                                                 int newBalance,
                                                                 String operationType,
                                                                 Long transactionId) {
        return AdminCreditAdjustmentResultVO.builder()
                .userId(targetUser.getId())
                .email(targetUser.getEmail())
                .adjustedAmount(adjustedAmount)
                .changeAmount(changeAmount)
                .newBalance(newBalance)
                .operationType(operationType)
                .transactionId(transactionId)
                .build();
    }

    private String buildAdminRemark(String action, String reason, User adminUser) {
        return action + "：" + reason.trim()
                + "；adminId=" + adminUser.getId()
                + "；adminEmail=" + adminUser.getEmail();
    }

    private AdminCreditTransactionVO toAdminTransactionVO(CreditLog log) {
        String operationType = log.getOperationType();
        return AdminCreditTransactionVO.builder()
                .id(log.getId())
                .amount(log.getChangeAmount())
                .type(classifyTransactionType(log))
                .module(operationType)
                .description(log.getRemark())
                .balanceAfter(log.getAfterAmount())
                .createdAt(log.getCreateTime())
                .build();
    }

    private String classifyTransactionType(CreditLog log) {
        String operationType = log.getOperationType();
        if (operationType != null && operationType.contains("REFUND")) {
            return "REFUND";
        }
        if (CreditCostConstants.OP_ADMIN_GRANT.equals(operationType) || "REGISTER_GIFT".equals(operationType)) {
            return "GRANT";
        }
        if (CreditCostConstants.OP_ADMIN_DEDUCT.equals(operationType) || safeAmount(log) < 0) {
            return "CONSUME";
        }
        return "ADJUSTMENT";
    }

    private int safeAmount(CreditLog log) {
        return log.getChangeAmount() == null ? 0 : log.getChangeAmount();
    }

    private int currentBalance(UserPlan userPlan) {
        return userPlan.getRemainingCredits() == null ? 0 : userPlan.getRemainingCredits();
    }

    private int sanitizePage(Integer page) {
        return page == null ? 1 : Math.max(1, page);
    }

    private int sanitizeSize(Integer size) {
        int safeSize = size == null ? DEFAULT_PAGE_SIZE : size;
        return Math.max(1, Math.min(safeSize, MAX_PAGE_SIZE));
    }

    private CreditUserSort parseSort(String sort) {
        if (!StringUtils.hasText(sort)) {
            return new CreditUserSort("u.create_time", "DESC");
        }

        return switch (sort.trim()) {
            case "balanceAsc" -> new CreditUserSort("credit_balance", "ASC");
            case "balanceDesc" -> new CreditUserSort("credit_balance", "DESC");
            case "createdAtAsc" -> new CreditUserSort("u.create_time", "ASC");
            case "lastCreditChangeAtAsc" -> new CreditUserSort("last_credit_change_at", "ASC");
            case "lastCreditChangeAtDesc" -> new CreditUserSort("last_credit_change_at", "DESC");
            default -> new CreditUserSort("u.create_time", "DESC");
        };
    }

    private User requireExistingUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户 ID 不能为空");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "目标用户不存在");
        }
        return user;
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

    private record CreditUserSort(String column, String direction) {
    }
}
