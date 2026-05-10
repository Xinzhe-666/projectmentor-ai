package com.xinzhe.projectmentor.analysis.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AnalysisReportVO {

    private Long id;

    private Long projectId;

    private Integer totalScore;

    private Integer runnabilityScore;

    private Integer authenticityScore;

    private Integer structureScore;

    private Integer readmeScore;

    private Integer securityScore;

    private Integer engineeringScore;

    private Integer interviewScore;

    private String summary;

    private String strengths;

    private String weaknesses;

    private String riskPoints;

    private String evidenceChain;

    private String suggestions;

    private String resumeBasic;

    private String resumeStandard;

    private String resumeAdvanced;

    private LocalDateTime createTime;
}