package com.xinzhe.projectmentor.share.vo;

import com.xinzhe.projectmentor.claim.vo.ClaimEvidenceAiEnhancementVO;
import com.xinzhe.projectmentor.claim.vo.ClaimEvidenceItemVO;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PublicReportVO {

    private String projectName;

    private String projectType;

    private String techStack;

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

    private List<ClaimEvidenceItemVO> claimEvidenceList;

    private ClaimEvidenceAiEnhancementVO claimEvidenceAi;

    private String suggestions;

    private String resumeBasic;

    private String resumeStandard;

    private String resumeAdvanced;

    private LocalDateTime createTime;
}
