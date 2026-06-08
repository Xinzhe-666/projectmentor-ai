package com.xinzhe.projectmentor.claim.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimEvidenceAiItemVO {

    private String claimText;

    private String aiExplanation;

    private String saferResumeExpression;

    private List<String> likelyInterviewQuestions;

    private String improvementSuggestion;
}
