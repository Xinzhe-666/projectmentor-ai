package com.xinzhe.projectmentor.project.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProjectVO {

    private Long id;

    private String name;

    private String githubUrl;

    private String description;

    private String projectType;

    private String techStack;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}