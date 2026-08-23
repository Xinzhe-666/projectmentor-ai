package com.xinzhe.projectmentor.defense.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DefenseAnswerResponse {

    private Long id;

    private Long questionId;

    private String answerText;

    private String evaluationStatus;

    private DefenseReviewResult reviewResult;

    private LocalDateTime createdAt;
}
