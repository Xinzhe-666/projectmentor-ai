package com.xinzhe.projectmentor.credit.service;

import com.xinzhe.projectmentor.admin.service.AdminService;
import com.xinzhe.projectmentor.auth.entity.User;
import com.xinzhe.projectmentor.auth.mapper.UserMapper;
import com.xinzhe.projectmentor.common.BusinessException;
import com.xinzhe.projectmentor.common.ErrorCode;
import com.xinzhe.projectmentor.credit.dto.AdminCreditAdjustmentRequest;
import com.xinzhe.projectmentor.credit.entity.CreditLog;
import com.xinzhe.projectmentor.credit.entity.UserPlan;
import com.xinzhe.projectmentor.credit.mapper.AdminCreditQueryMapper;
import com.xinzhe.projectmentor.credit.mapper.CreditLogMapper;
import com.xinzhe.projectmentor.credit.mapper.UserPlanMapper;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreditServiceConcurrencyTests {

    @Test
    void mapperLocksBalanceRowWithSelectForUpdate() throws Exception {
        Select select = UserPlanMapper.class
                .getMethod("selectByUserIdForUpdate", Long.class)
                .getAnnotation(Select.class);

        assertThat(select).isNotNull();
        String sql = String.join(" ", Arrays.asList(select.value()))
                .replaceAll("\\s+", " ")
                .trim();
        assertThat(sql)
                .contains("FROM pm_user_plan")
                .contains("WHERE user_id = #{userId}")
                .endsWith("FOR UPDATE");
    }

    @Test
    void successfulConsumptionUsesLockedBalanceAndKeepsLogAmountsConsistent() {
        Fixture fixture = fixtureWithLockedBalance(10);

        fixture.service.consumeCredits(7L, 2, "TEST_CONSUME", 42L, "并发扣款测试");

        verify(fixture.userPlanMapper).selectByUserIdForUpdate(7L);
        ArgumentCaptor<UserPlan> planCaptor = ArgumentCaptor.forClass(UserPlan.class);
        ArgumentCaptor<CreditLog> logCaptor = ArgumentCaptor.forClass(CreditLog.class);
        verify(fixture.userPlanMapper).updateById(planCaptor.capture());
        verify(fixture.creditLogMapper).insert(logCaptor.capture());

        assertThat(planCaptor.getValue().getRemainingCredits()).isEqualTo(8);
        assertThat(logCaptor.getValue().getBeforeAmount()).isEqualTo(10);
        assertThat(logCaptor.getValue().getChangeAmount()).isEqualTo(-2);
        assertThat(logCaptor.getValue().getAfterAmount()).isEqualTo(8);
    }

    @Test
    void insufficientLockedBalanceDoesNotUpdateOrWriteLog() {
        Fixture fixture = fixtureWithLockedBalance(1);

        assertThatThrownBy(() -> fixture.service.consumeCredits(
                7L, 2, "TEST_CONSUME", 42L, "余额不足测试"
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.CREDIT_NOT_ENOUGH.getCode());

        verify(fixture.userPlanMapper).selectByUserIdForUpdate(7L);
        verify(fixture.userPlanMapper, never()).updateById(any(UserPlan.class));
        verify(fixture.creditLogMapper, never()).insert(any(CreditLog.class));
    }

    @Test
    void refundUsesLatestLockedBalanceInsteadOfStaleRead() {
        Fixture fixture = fixtureWithLockedBalance(8);
        UserPlan stalePlan = plan(7L, 5);
        when(fixture.userPlanMapper.selectOne(any())).thenReturn(stalePlan);

        fixture.service.refundCredits(7L, 2, "TEST_REFUND", 42L, "退款测试");

        ArgumentCaptor<CreditLog> logCaptor = ArgumentCaptor.forClass(CreditLog.class);
        verify(fixture.creditLogMapper).insert(logCaptor.capture());
        verify(fixture.userPlanMapper).selectByUserIdForUpdate(7L);
        assertThat(logCaptor.getValue().getBeforeAmount()).isEqualTo(8);
        assertThat(logCaptor.getValue().getAfterAmount()).isEqualTo(10);
    }

    @Test
    void refundRejectsIntegerOverflowBeforeWritingBalanceOrLog() {
        Fixture fixture = fixtureWithLockedBalance(Integer.MAX_VALUE);

        assertThatThrownBy(() -> fixture.service.refundCredits(
                7L, 1, "TEST_REFUND", 42L, "溢出保护测试"
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage("额度余额已达到系统上限");

        verify(fixture.userPlanMapper, never()).updateById(any(UserPlan.class));
        verify(fixture.creditLogMapper, never()).insert(any(CreditLog.class));
    }

    @Test
    void adminGrantAndDeductionUseSameLockedBalancePath() {
        Fixture grantFixture = fixtureWithLockedBalance(10);
        grantFixture.service.grantCreditsByAdmin(7L, adminRequest(5, "并发发放"));
        verify(grantFixture.userPlanMapper).selectByUserIdForUpdate(7L);
        ArgumentCaptor<CreditLog> grantLog = ArgumentCaptor.forClass(CreditLog.class);
        verify(grantFixture.creditLogMapper).insert(grantLog.capture());
        assertThat(grantLog.getValue().getBeforeAmount()).isEqualTo(10);
        assertThat(grantLog.getValue().getAfterAmount()).isEqualTo(15);

        Fixture deductFixture = fixtureWithLockedBalance(10);
        deductFixture.service.deductCreditsByAdmin(7L, adminRequest(4, "并发扣除"));
        verify(deductFixture.userPlanMapper).selectByUserIdForUpdate(7L);
        ArgumentCaptor<CreditLog> deductLog = ArgumentCaptor.forClass(CreditLog.class);
        verify(deductFixture.creditLogMapper).insert(deductLog.capture());
        assertThat(deductLog.getValue().getBeforeAmount()).isEqualTo(10);
        assertThat(deductLog.getValue().getAfterAmount()).isEqualTo(6);
    }

    @Test
    void duplicatePlanCreationReloadsAndLocksConcurrentPlan() {
        Fixture fixture = fixture();
        UserPlan concurrentPlan = plan(7L, 10);
        when(fixture.userPlanMapper.selectByUserIdForUpdate(7L))
                .thenReturn(null, concurrentPlan);
        doThrow(new DuplicateKeyException("uk_user_id database detail"))
                .when(fixture.userPlanMapper).insert(any(UserPlan.class));

        fixture.service.consumeCredits(7L, 2, "TEST_CONSUME", 42L, "创建竞争测试");

        verify(fixture.userPlanMapper, times(2)).selectByUserIdForUpdate(7L);
        verify(fixture.userPlanMapper).insert(any(UserPlan.class));
        ArgumentCaptor<CreditLog> logCaptor = ArgumentCaptor.forClass(CreditLog.class);
        verify(fixture.creditLogMapper).insert(logCaptor.capture());
        assertThat(logCaptor.getValue().getBeforeAmount()).isEqualTo(10);
        assertThat(logCaptor.getValue().getAfterAmount()).isEqualTo(8);
    }

    @Test
    void failedBalanceUpdateDoesNotWriteCreditLog() {
        Fixture fixture = fixtureWithLockedBalance(10);
        when(fixture.userPlanMapper.updateById(any(UserPlan.class))).thenReturn(0);

        assertThatThrownBy(() -> fixture.service.consumeCredits(
                7L, 2, "TEST_CONSUME", 42L, "更新失败测试"
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage("额度余额更新失败");

        verify(fixture.creditLogMapper, never()).insert(any(CreditLog.class));
    }

    @Test
    void creditLogFailurePropagatesFromRollbackConfiguredTransaction() throws Exception {
        Fixture fixture = fixtureWithLockedBalance(10);
        doReturn(0).when(fixture.creditLogMapper).insert(any(CreditLog.class));

        assertThatThrownBy(() -> fixture.service.consumeCredits(
                7L, 2, "TEST_CONSUME", 42L, "流水失败测试"
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage("额度流水写入失败");
        verify(fixture.userPlanMapper).updateById(any(UserPlan.class));

        Method method = CreditService.class.getMethod(
                "consumeCredits", Long.class, int.class, String.class, Long.class, String.class
        );
        Transactional transactional = method.getAnnotation(Transactional.class);
        assertThat(transactional).isNotNull();
        assertThat(transactional.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
        assertThat(transactional.rollbackFor()).containsExactly(Exception.class);
    }

    @Test
    void refundAndAdminMutationsAreRollbackConfiguredTransactions() throws Exception {
        Method refund = CreditService.class.getMethod(
                "refundCredits", Long.class, int.class, String.class, Long.class, String.class
        );
        Method grant = CreditService.class.getMethod(
                "grantCreditsByAdmin", Long.class, AdminCreditAdjustmentRequest.class
        );
        Method deduct = CreditService.class.getMethod(
                "deductCreditsByAdmin", Long.class, AdminCreditAdjustmentRequest.class
        );

        assertThat(refund.getAnnotation(Transactional.class).propagation())
                .isEqualTo(Propagation.REQUIRES_NEW);
        assertThat(refund.getAnnotation(Transactional.class).rollbackFor())
                .containsExactly(Exception.class);
        assertThat(grant.getAnnotation(Transactional.class).rollbackFor())
                .containsExactly(Exception.class);
        assertThat(deduct.getAnnotation(Transactional.class).rollbackFor())
                .containsExactly(Exception.class);
    }

    @Test
    void taskDebitUsesStableDatabaseBackedIdempotencyKey() {
        Fixture fixture = fixtureWithLockedBalance(10);

        assertThat(fixture.service.consumeCreditsOnceForTask(
                7L, 2, "AI_AUDIT_REPORT", 100L, "task debit"
        )).isTrue();

        ArgumentCaptor<CreditLog> logCaptor = ArgumentCaptor.forClass(CreditLog.class);
        verify(fixture.creditLogMapper).insert(logCaptor.capture());
        assertThat(logCaptor.getValue().getIdempotencyKey()).isEqualTo("ANALYSIS_DEBIT:100");

        CreditLog existing = new CreditLog();
        existing.setIdempotencyKey("ANALYSIS_DEBIT:100");
        when(fixture.creditLogMapper.selectByIdempotencyKey("ANALYSIS_DEBIT:100")).thenReturn(existing);
        assertThat(fixture.service.consumeCreditsOnceForTask(
                7L, 2, "AI_AUDIT_REPORT", 100L, "duplicate"
        )).isFalse();
    }

    @Test
    void taskRefundRequiresDebitAndUsesIndependentStableKey() {
        Fixture noDebit = fixtureWithLockedBalance(8);
        assertThat(noDebit.service.refundCreditsOnceForTask(
                7L, "AI_AUDIT_REPORT_REFUND", 100L, "no debit"
        )).isFalse();
        verify(noDebit.userPlanMapper, never()).updateById(any(UserPlan.class));
        verify(noDebit.creditLogMapper, never()).insert(any(CreditLog.class));

        Fixture debited = fixtureWithLockedBalance(8);
        CreditLog debit = new CreditLog();
        debit.setChangeAmount(-2);
        when(debited.creditLogMapper.selectByIdempotencyKey("ANALYSIS_DEBIT:100")).thenReturn(debit);
        assertThat(debited.service.refundCreditsOnceForTask(
                7L, "AI_AUDIT_REPORT_REFUND", 100L, "refund"
        )).isTrue();

        ArgumentCaptor<CreditLog> refund = ArgumentCaptor.forClass(CreditLog.class);
        verify(debited.creditLogMapper).insert(refund.capture());
        assertThat(refund.getValue().getIdempotencyKey()).isEqualTo("ANALYSIS_REFUND:100");
        assertThat(refund.getValue().getChangeAmount()).isEqualTo(2);
        assertThat(refund.getValue().getAfterAmount()).isEqualTo(10);
    }

    private Fixture fixtureWithLockedBalance(int balance) {
        Fixture fixture = fixture();
        when(fixture.userPlanMapper.selectByUserIdForUpdate(7L)).thenReturn(plan(7L, balance));
        return fixture;
    }

    private Fixture fixture() {
        UserPlanMapper userPlanMapper = mock(UserPlanMapper.class);
        CreditLogMapper creditLogMapper = mock(CreditLogMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        AdminService adminService = mock(AdminService.class);
        AdminCreditQueryMapper adminCreditQueryMapper = mock(AdminCreditQueryMapper.class);

        when(userPlanMapper.updateById(any(UserPlan.class))).thenReturn(1);
        doAnswer(invocation -> {
            CreditLog log = invocation.getArgument(0);
            log.setId(100L);
            return 1;
        }).when(creditLogMapper).insert(any(CreditLog.class));

        User admin = new User();
        admin.setId(1L);
        admin.setEmail("admin@example.com");
        when(adminService.requireAdminUser()).thenReturn(admin);

        User target = new User();
        target.setId(7L);
        target.setUsername("tester");
        target.setEmail("tester@example.com");
        when(userMapper.selectById(7L)).thenReturn(target);

        CreditService service = new CreditService(
                userPlanMapper,
                creditLogMapper,
                userMapper,
                adminService,
                adminCreditQueryMapper
        );
        return new Fixture(service, userPlanMapper, creditLogMapper);
    }

    private UserPlan plan(Long userId, int balance) {
        UserPlan plan = new UserPlan();
        plan.setId(9L);
        plan.setUserId(userId);
        plan.setPlanType("FREE");
        plan.setRemainingCredits(balance);
        return plan;
    }

    private AdminCreditAdjustmentRequest adminRequest(int amount, String reason) {
        AdminCreditAdjustmentRequest request = new AdminCreditAdjustmentRequest();
        request.setAmount(amount);
        request.setReason(reason);
        return request;
    }

    private record Fixture(CreditService service,
                           UserPlanMapper userPlanMapper,
                           CreditLogMapper creditLogMapper) {
    }
}
