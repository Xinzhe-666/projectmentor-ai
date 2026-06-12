package com.xinzhe.projectmentor.admin.vo;

import lombok.Data;

@Data
public class AiUsageAggregateRow {

    private Long aiCalls;

    private Long creditsConsumed;

    private Long refundCount;

    private Long refundCredits;
}
