package com.xinzhe.projectmentor.admin.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminRecentReportVO {

    private Long id;

    private Long projectId;

    private Long userId;

    private Integer totalScore;

    private LocalDateTime createTime;
}
