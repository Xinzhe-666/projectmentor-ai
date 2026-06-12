package com.xinzhe.projectmentor.admin.vo;

import lombok.Data;

@Data
public class AdminAiUsageUserVO {

    private Long userId;

    private String username;

    private String email;

    private Long aiCalls;

    private Long creditsConsumed;
}
