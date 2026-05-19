package com.xinzhe.projectmentor.analysis.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AnalysisTaskVO {

    private Long taskId;

    private Long projectId;

    private String taskType;

    private String status;

    private Integer progress;

    private Long reportId;

    private String failReason;

    private String message;

    private LocalDateTime createTime;

    private LocalDateTime finishTime;
}