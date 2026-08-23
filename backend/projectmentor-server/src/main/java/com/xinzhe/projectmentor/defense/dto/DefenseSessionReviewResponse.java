package com.xinzhe.projectmentor.defense.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DefenseSessionReviewResponse {

    private DefenseSessionResponse session;

    private List<DefenseQuestionResponse> questions;

    private Integer answeredCount;

    private Integer supportedCount;

    private Integer partialCount;

    private Integer insufficientCount;
}
