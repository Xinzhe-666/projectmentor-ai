package com.xinzhe.projectmentor.claim.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimEvidenceAiEnhancementVO {

    private Boolean aiEnhanced;

    private LocalDateTime aiEnhancedAt;

    private String aiSummary;

    private String aiRiskOverview;

    private String aiResumeStrategy;

    private String aiInterviewStrategy;

    private List<ClaimEvidenceAiItemVO> aiEnhancedItems;

    private String aiFallbackText;
}
