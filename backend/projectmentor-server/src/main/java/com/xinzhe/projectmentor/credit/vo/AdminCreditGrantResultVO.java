package com.xinzhe.projectmentor.credit.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminCreditGrantResultVO {

    private Long userId;

    private String email;

    private Integer grantedAmount;

    private Integer newBalance;

    private Long transactionId;
}
