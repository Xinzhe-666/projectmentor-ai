package com.xinzhe.projectmentor.credit.service;

import com.xinzhe.projectmentor.admin.service.AdminService;
import com.xinzhe.projectmentor.auth.entity.User;
import com.xinzhe.projectmentor.auth.mapper.UserMapper;
import com.xinzhe.projectmentor.common.BusinessException;
import com.xinzhe.projectmentor.credit.CreditCostConstants;
import com.xinzhe.projectmentor.credit.dto.AdminCreditAdjustmentRequest;
import com.xinzhe.projectmentor.credit.entity.CreditLog;
import com.xinzhe.projectmentor.credit.entity.UserPlan;
import com.xinzhe.projectmentor.credit.mapper.AdminCreditQueryMapper;
import com.xinzhe.projectmentor.credit.mapper.CreditLogMapper;
import com.xinzhe.projectmentor.credit.mapper.UserPlanMapper;
import com.xinzhe.projectmentor.credit.vo.AdminCreditAdjustmentResultVO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreditServiceAdminTests {

    @Test
    void adminCanGrantCreditsAndWriteLog() {
        TestFixture fixture = createFixture(10);

        AdminCreditAdjustmentResultVO result = fixture.service.grantCreditsByAdmin(
                7L, request(5, "内测奖励")
        );

        ArgumentCaptor<CreditLog> logCaptor = ArgumentCaptor.forClass(CreditLog.class);
        verify(fixture.creditLogMapper).insert(logCaptor.capture());

        assertThat(result.getNewBalance()).isEqualTo(15);
        assertThat(result.getChangeAmount()).isEqualTo(5);
        assertThat(result.getOperationType()).isEqualTo(CreditCostConstants.OP_ADMIN_GRANT);
        assertThat(logCaptor.getValue().getOperationType()).isEqualTo(CreditCostConstants.OP_ADMIN_GRANT);
        assertThat(logCaptor.getValue().getRemark()).contains("内测奖励").contains("adminId=1");
    }

    @Test
    void adminCanDeductCreditsAndWriteLog() {
        TestFixture fixture = createFixture(10);

        AdminCreditAdjustmentResultVO result = fixture.service.deductCreditsByAdmin(
                7L, request(4, "异常使用扣除")
        );

        ArgumentCaptor<CreditLog> logCaptor = ArgumentCaptor.forClass(CreditLog.class);
        verify(fixture.creditLogMapper).insert(logCaptor.capture());

        assertThat(result.getNewBalance()).isEqualTo(6);
        assertThat(result.getChangeAmount()).isEqualTo(-4);
        assertThat(result.getOperationType()).isEqualTo(CreditCostConstants.OP_ADMIN_DEDUCT);
        assertThat(logCaptor.getValue().getOperationType()).isEqualTo(CreditCostConstants.OP_ADMIN_DEDUCT);
        assertThat(logCaptor.getValue().getAfterAmount()).isEqualTo(6);
    }

    @Test
    void grantRejectsNonPositiveAmount() {
        TestFixture fixture = createFixture(10);

        assertThatThrownBy(() -> fixture.service.grantCreditsByAdmin(
                7L, request(0, "无效发放")
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("正整数");

        verify(fixture.userPlanMapper, never()).updateById(any(UserPlan.class));
        verify(fixture.creditLogMapper, never()).insert(any(CreditLog.class));
    }

    @Test
    void deductionRejectsNonPositiveAmount() {
        TestFixture fixture = createFixture(10);

        assertThatThrownBy(() -> fixture.service.deductCreditsByAdmin(
                7L, request(-1, "无效扣除")
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("正整数");

        verify(fixture.userPlanMapper, never()).updateById(any(UserPlan.class));
        verify(fixture.creditLogMapper, never()).insert(any(CreditLog.class));
    }

    @Test
    void deductionCannotExceedBalance() {
        TestFixture fixture = createFixture(3);

        assertThatThrownBy(() -> fixture.service.deductCreditsByAdmin(
                7L, request(4, "超额扣除")
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能超过当前余额");

        verify(fixture.userPlanMapper, never()).updateById(any(UserPlan.class));
        verify(fixture.creditLogMapper, never()).insert(any(CreditLog.class));
    }

    private TestFixture createFixture(int balance) {
        UserPlanMapper userPlanMapper = mock(UserPlanMapper.class);
        CreditLogMapper creditLogMapper = mock(CreditLogMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        AdminService adminService = mock(AdminService.class);
        AdminCreditQueryMapper adminCreditQueryMapper = mock(AdminCreditQueryMapper.class);

        User admin = new User();
        admin.setId(1L);
        admin.setEmail("admin@example.com");
        when(adminService.requireAdminUser()).thenReturn(admin);

        User target = new User();
        target.setId(7L);
        target.setUsername("tester");
        target.setEmail("tester@example.com");
        when(userMapper.selectById(7L)).thenReturn(target);

        UserPlan plan = new UserPlan();
        plan.setId(9L);
        plan.setUserId(7L);
        plan.setPlanType("FREE");
        plan.setRemainingCredits(balance);
        when(userPlanMapper.selectOne(any())).thenReturn(plan);
        when(userPlanMapper.updateById(any(UserPlan.class))).thenReturn(1);
        doAnswer(invocation -> {
            CreditLog log = invocation.getArgument(0);
            log.setId(100L);
            return 1;
        }).when(creditLogMapper).insert(any(CreditLog.class));

        CreditService service = new CreditService(
                userPlanMapper,
                creditLogMapper,
                userMapper,
                adminService,
                adminCreditQueryMapper
        );
        return new TestFixture(service, userPlanMapper, creditLogMapper);
    }

    private AdminCreditAdjustmentRequest request(int amount, String reason) {
        AdminCreditAdjustmentRequest request = new AdminCreditAdjustmentRequest();
        request.setAmount(amount);
        request.setReason(reason);
        return request;
    }

    private record TestFixture(CreditService service,
                               UserPlanMapper userPlanMapper,
                               CreditLogMapper creditLogMapper) {
    }
}
