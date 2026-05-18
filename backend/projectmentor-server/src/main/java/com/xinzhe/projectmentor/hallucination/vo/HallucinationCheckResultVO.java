package com.xinzhe.projectmentor.hallucination.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class HallucinationCheckResultVO {

    private Integer credibilityScore;

    private Integer objectivityScore;

    private String riskLevel;

    private Boolean overEncouragementRisk;

    private Boolean missingEvidenceRisk;

    private Boolean resumeRisk;

    private Integer issueCount;

    private List<HallucinationIssueVO> issues;

    private List<String> unsafeResumeStatements;

    private String saferRewrite;
}