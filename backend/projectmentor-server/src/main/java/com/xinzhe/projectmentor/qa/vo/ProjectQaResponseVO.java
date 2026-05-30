package com.xinzhe.projectmentor.qa.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProjectQaResponseVO {

    private String question;

    private String answer;

    private Boolean aiUsed;

    private List<ProjectQaEvidenceVO> evidences;

    private List<String> suggestedFollowUps;

    private String evidenceLevel;

    private String evidenceLevelText;

    private String evidenceSummary;

    private String interviewAnswer;

    private String resumeRisk;

    private Integer confidenceScore;
}
