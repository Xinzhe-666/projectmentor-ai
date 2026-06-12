package com.xinzhe.projectmentor.admin.service;

import com.xinzhe.projectmentor.admin.vo.AdminAiUsageOverviewVO;
import com.xinzhe.projectmentor.admin.vo.AiUsageAggregateRow;
import com.xinzhe.projectmentor.credit.mapper.CreditLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AdminAiUsageService {

    private final CreditLogMapper creditLogMapper;

    private final AdminService adminService;

    public AdminAiUsageOverviewVO getOverview() {
        adminService.requireAdmin();

        var todayStart = LocalDate.now().atStartOfDay();
        AiUsageAggregateRow today = nonNullAggregate(creditLogMapper.selectAiUsageAggregate(todayStart));
        AiUsageAggregateRow total = nonNullAggregate(creditLogMapper.selectAiUsageAggregate(null));

        return AdminAiUsageOverviewVO.builder()
                .todayAiCalls(today.getAiCalls())
                .todayCreditsConsumed(today.getCreditsConsumed())
                .todayRefundCount(today.getRefundCount())
                .todayRefundCredits(today.getRefundCredits())
                .totalAiCalls(total.getAiCalls())
                .totalCreditsConsumed(total.getCreditsConsumed())
                .totalRefundCount(total.getRefundCount())
                .totalRefundCredits(total.getRefundCredits())
                .topModulesToday(creditLogMapper.selectTopAiModules(todayStart))
                .topUsersToday(creditLogMapper.selectTopAiUsers(todayStart))
                .recentCreditLogs(creditLogMapper.selectRecentAiCreditLogs())
                .build();
    }

    private AiUsageAggregateRow nonNullAggregate(AiUsageAggregateRow row) {
        if (row == null) {
            row = new AiUsageAggregateRow();
        }
        row.setAiCalls(row.getAiCalls() == null ? 0L : row.getAiCalls());
        row.setCreditsConsumed(row.getCreditsConsumed() == null ? 0L : row.getCreditsConsumed());
        row.setRefundCount(row.getRefundCount() == null ? 0L : row.getRefundCount());
        row.setRefundCredits(row.getRefundCredits() == null ? 0L : row.getRefundCredits());
        return row;
    }
}
