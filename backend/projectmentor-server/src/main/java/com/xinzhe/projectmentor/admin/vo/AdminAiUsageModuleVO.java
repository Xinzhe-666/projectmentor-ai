package com.xinzhe.projectmentor.admin.vo;

import lombok.Data;

@Data
public class AdminAiUsageModuleVO {

    private String module;

    private Long aiCalls;

    private Long creditsConsumed;
}
