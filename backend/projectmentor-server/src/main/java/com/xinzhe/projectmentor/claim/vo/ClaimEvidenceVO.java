package com.xinzhe.projectmentor.claim.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class ClaimEvidenceVO {

    private Long projectId;

    private Integer totalClaims;

    private Map<String, Integer> statusCounts;

    private List<ClaimEvidenceItemVO> items;
}
