package com.xinzhe.projectmentor.defense.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DefenseSessionResponse {

    private Long id;

    private Long projectId;

    private Long reportId;

    private String mode;

    private String status;

    private Integer questionCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
