package com.xinzhe.projectmentor.claim.vo;

import com.xinzhe.projectmentor.claim.ClaimCategory;
import com.xinzhe.projectmentor.claim.ClaimEvidenceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimEvidenceItemVO {

    private String claimText;

    private String sourceType;

    private String sourceSnippet;

    private ClaimCategory category;

    private ClaimEvidenceStatus status;

    private Integer confidenceScore;

    private String reason;

    private List<ClaimEvidenceFileVO> evidenceFiles;

    private String resumeAdvice;

    private String interviewQuestion;
}
