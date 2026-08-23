package com.xinzhe.projectmentor.defense.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DefenseQuestionResponse {

    private Long id;

    private Long sessionId;

    private String question;

    private String category;

    private List<String> relatedClaims;

    private List<DefenseEvidenceReference> relatedEvidence;

    private Integer sortOrder;

    private DefenseAnswerResponse answer;
}
