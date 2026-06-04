package com.xinzhe.projectmentor.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateProjectRequest {

    @NotBlank(message = "项目名称不能为空")
    @Size(max = 100, message = "项目名称不能超过 100 个字符")
    private String name;

    @Size(max = 255, message = "GitHub 地址不能超过 255 个字符")
    private String githubUrl;

    @Size(max = 10000, message = "项目描述不能超过 10000 个字符")
    private String description;

    @Size(max = 50, message = "项目类型不能超过 50 个字符")
    private String projectType;

    @Size(max = 5000, message = "技术栈不能超过 5000 个字符")
    private String techStack;
}
