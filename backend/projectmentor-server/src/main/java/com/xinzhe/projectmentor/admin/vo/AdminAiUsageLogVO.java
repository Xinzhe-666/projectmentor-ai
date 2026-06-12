package com.xinzhe.projectmentor.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminAiUsageLogVO {

    private Long id;

    private Long userId;

    private String username;

    private Integer amount;

    private String type;

    private String module;

    private String description;

    private Integer balanceAfter;

    private LocalDateTime createdAt;
}
