package com.xinzhe.projectmentor.interview.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SubmitAnswerRequest {

    @NotBlank(message = "回答内容不能为空")
    @Size(max = 10000, message = "回答内容不能超过 10000 个字符")
    private String answer;
}