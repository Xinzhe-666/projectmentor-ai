package com.xinzhe.projectmentor.feedback.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminFeedbackStatusRequest {

    @NotBlank(message = "不能为空")
    private String status;

    @Size(max = 1000, message = "长度不能超过 1000 个字符")
    private String adminNote;
}
