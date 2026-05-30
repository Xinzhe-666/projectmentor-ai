package com.xinzhe.projectmentor.qa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProjectQaRequest {

    @NotBlank(message = "问题不能为空")
    @Size(min = 2, max = 1000, message = "问题长度应为 2 到 1000 个字符")
    private String question;
}
