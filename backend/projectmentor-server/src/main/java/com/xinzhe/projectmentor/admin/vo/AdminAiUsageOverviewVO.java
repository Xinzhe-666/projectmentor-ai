package com.xinzhe.projectmentor.admin.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AdminAiUsageOverviewVO {

    private Long todayAiCalls;

    private Long todayCreditsConsumed;

    private Long todayRefundCount;

    private Long todayRefundCredits;

    private Long totalAiCalls;

    private Long totalCreditsConsumed;

    private Long totalRefundCount;

    private Long totalRefundCredits;

    private List<AdminAiUsageModuleVO> topModulesToday;

    private List<AdminAiUsageUserVO> topUsersToday;

    private List<AdminAiUsageLogVO> recentCreditLogs;
}
