package com.xinzhe.projectmentor.credit.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminCreditTransactionVO {

    private Long id;

    private Integer changeAmount;

    private String type;

    private String reason;

    private LocalDateTime createTime;
}
