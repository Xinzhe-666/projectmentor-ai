package com.xinzhe.projectmentor.credit.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminCreditAdjustmentResultVO {

    private Long userId;

    private String email;

    private Integer adjustedAmount;

    private Integer changeAmount;

    private Integer newBalance;

    private String operationType;

    private Long transactionId;
}
