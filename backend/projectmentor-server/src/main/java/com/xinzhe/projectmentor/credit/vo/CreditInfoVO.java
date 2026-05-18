package com.xinzhe.projectmentor.credit.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CreditInfoVO {

    private Long userId;

    private String planType;

    private Integer remainingCredits;

    private LocalDateTime expireTime;
}