package com.xinzhe.projectmentor.admin.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminStatsVO {

    private Long userCount;

    private Long projectCount;

    private Long reportCount;

    private Long qaCount;

    private Long shareCount;

    private Long todayUserCount;

    private Long todayProjectCount;

    private Long todayReportCount;

    private Long todayQaCount;
}
