package com.xinzhe.projectmentor.credit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xinzhe.projectmentor.auth.entity.User;
import com.xinzhe.projectmentor.auth.interceptor.UserContext;
import com.xinzhe.projectmentor.auth.mapper.UserMapper;
import com.xinzhe.projectmentor.common.BusinessException;
import com.xinzhe.projectmentor.common.ErrorCode;
import com.xinzhe.projectmentor.credit.dto.AddCreditRequest;
import com.xinzhe.projectmentor.credit.entity.CreditLog;
import com.xinzhe.projectmentor.credit.entity.UserPlan;
import com.xinzhe.projectmentor.credit.mapper.CreditLogMapper;
import com.xinzhe.projectmentor.credit.mapper.UserPlanMapper;
import com.xinzhe.projectmentor.credit.vo.CreditInfoVO;
import com.xinzhe.projectmentor.credit.vo.CreditLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CreditService {

    private final UserPlanMapper userPlanMapper;

    private final CreditLogMapper creditLogMapper;

    private final UserMapper userMapper;

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
        checkCurrentUserIsAdmin();

        User targetUser = userMapper.selectById(request.getUserId());

        if (targetUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "目标用户不存在");
        }

        UserPlan userPlan = getOrCreateUserPlan(request.getUserId());

        int before = userPlan.getRemainingCredits() == null ? 0 : userPlan.getRemainingCredits();
        int after = before + request.getAmount();

        userPlan.setRemainingCredits(after);
        userPlanMapper.updateById(userPlan);

        createCreditLog(
                request.getUserId(),
                request.getAmount(),
                before,
                after,
                "ADMIN_ADD",
                null,
                request.getRemark() == null || request.getRemark().isBlank()
                        ? "管理员手动增加额度"
                        : request.getRemark()
        );

        return CreditInfoVO.builder()
                .userId(request.getUserId())
                .planType(userPlan.getPlanType())
                .remainingCredits(after)
                .expireTime(userPlan.getExpireTime())
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
            throw new BusinessException(ErrorCode.CREDIT_NOT_ENOUGH, "额度不足，当前剩余额度：" + before);
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

    private void createCreditLog(Long userId,
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
    }

    private void checkCurrentUserIsAdmin() {
        Long currentUserId = getCurrentUserId();

        User currentUser = userMapper.selectById(currentUserId);

        if (currentUser == null || !"ADMIN".equalsIgnoreCase(currentUser.getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有管理员可以增加额度");
        }
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