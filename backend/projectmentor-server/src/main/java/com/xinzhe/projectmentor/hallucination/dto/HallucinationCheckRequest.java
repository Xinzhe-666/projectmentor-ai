package com.xinzhe.projectmentor.hallucination.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class HallucinationCheckRequest {

    /**
     * 可选：如果传了 projectId，系统会结合该项目文件证据判断 AI 回答是否夸大。
     * 如果不传，就只对 AI 文本本身做通用风险检测。
     */
    private Long projectId;

    @NotBlank(message = "AI 回答内容不能为空")
    @Size(max = 30000, message = "AI 回答内容不能超过 30000 个字符")
    private String aiAnswer;
}