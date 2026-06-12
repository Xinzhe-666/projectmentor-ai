package com.xinzhe.projectmentor.admin.service;

import com.xinzhe.projectmentor.admin.vo.AdminAiUsageOverviewVO;
import com.xinzhe.projectmentor.admin.vo.AiUsageAggregateRow;
import com.xinzhe.projectmentor.credit.mapper.CreditLogMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminAiUsageServiceTests {

    @Test
    void overviewUsesCreditLogAggregates() {
        CreditLogMapper creditLogMapper = mock(CreditLogMapper.class);
        AdminService adminService = mock(AdminService.class);
        AiUsageAggregateRow today = aggregate(5, 8, 2, 3);
        AiUsageAggregateRow total = aggregate(20, 31, 4, 6);

        when(creditLogMapper.selectAiUsageAggregate(any(LocalDateTime.class))).thenReturn(today);
        when(creditLogMapper.selectAiUsageAggregate(null)).thenReturn(total);
        when(creditLogMapper.selectTopAiModules(any(LocalDateTime.class))).thenReturn(List.of());
        when(creditLogMapper.selectTopAiUsers(any(LocalDateTime.class))).thenReturn(List.of());
        when(creditLogMapper.selectRecentAiCreditLogs()).thenReturn(List.of());

        AdminAiUsageOverviewVO overview =
                new AdminAiUsageService(creditLogMapper, adminService).getOverview();

        verify(adminService).requireAdmin();
        assertThat(overview.getTodayAiCalls()).isEqualTo(5);
        assertThat(overview.getTodayCreditsConsumed()).isEqualTo(8);
        assertThat(overview.getTodayRefundCount()).isEqualTo(2);
        assertThat(overview.getTodayRefundCredits()).isEqualTo(3);
        assertThat(overview.getTotalAiCalls()).isEqualTo(20);
        assertThat(overview.getTotalCreditsConsumed()).isEqualTo(31);
    }

    private AiUsageAggregateRow aggregate(long calls, long consumed, long refunds, long refunded) {
        AiUsageAggregateRow row = new AiUsageAggregateRow();
        row.setAiCalls(calls);
        row.setCreditsConsumed(consumed);
        row.setRefundCount(refunds);
        row.setRefundCredits(refunded);
        return row;
    }
}
