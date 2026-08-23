package com.xinzhe.projectmentor.defense.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SubmitDefenseAnswerRequest {

    @NotBlank(message = "回答内容不能为空")
    @Size(max = 10000, message = "回答内容不能超过 10000 个字符")
    private String answerText;
}
