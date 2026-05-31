package com.xinzhe.projectmentor.admin.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminRecentQaVO {

    private Long id;

    private Long userId;

    private Long projectId;

    private String question;

    private Boolean aiUsed;

    private LocalDateTime createTime;
}
