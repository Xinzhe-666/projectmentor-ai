package com.xinzhe.projectmentor.interview.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StartInterviewRequest {

    @NotNull(message = "项目ID不能为空")
    private Long projectId;

    /**
     * HR_REALITY / TECH_DEEP_DIVE / PRESSURE / HUAWEI_BACKEND / AI_PROJECT
     */
    private String mode;
}