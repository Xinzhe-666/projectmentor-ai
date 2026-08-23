package com.xinzhe.projectmentor.defense.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DefenseReviewResult {

    private String evidenceAlignment;

    private String summary;

    private List<String> relatedClaims;

    private List<DefenseEvidenceReference> matchedEvidence;
}
