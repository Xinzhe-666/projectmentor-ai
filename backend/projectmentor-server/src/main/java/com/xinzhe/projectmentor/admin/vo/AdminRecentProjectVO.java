package com.xinzhe.projectmentor.admin.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminRecentProjectVO {

    private Long id;

    private Long userId;

    private String name;

    private String techStack;

    private String status;

    private LocalDateTime createTime;
}
