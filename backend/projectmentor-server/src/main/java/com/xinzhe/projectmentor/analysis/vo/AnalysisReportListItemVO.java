package com.xinzhe.projectmentor.analysis.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AnalysisReportListItemVO {

    private Long reportId;

    private Long projectId;

    private String projectName;

    private Integer authenticityScore;

    private Integer healthScore;

    private Integer totalScore;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Boolean shared;

    private String shareToken;

    private String summary;
}
