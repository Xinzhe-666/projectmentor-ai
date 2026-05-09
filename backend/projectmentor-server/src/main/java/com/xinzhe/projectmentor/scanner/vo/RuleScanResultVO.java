package com.xinzhe.projectmentor.scanner.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RuleScanResultVO {

    private Long projectId;

    private String projectName;

    private Boolean hasReadme;

    private Integer fileCount;

    private Integer totalRiskCount;

    private Integer highRiskCount;

    private Integer mediumRiskCount;

    private Integer lowRiskCount;

    private List<RiskItemVO> risks;

    private List<EvidenceItemVO> evidences;

    private List<String> suggestions;
}