package com.xinzhe.projectmentor.credit.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminCreditUserVO {

    private Long userId;

    private String username;

    private String email;

    private Integer creditBalance;

    private Long totalConsumed;

    private Long totalRefunded;

    private Long totalAdminGranted;

    private LocalDateTime createdAt;

    private LocalDateTime lastCreditChangeAt;
}
