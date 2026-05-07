package com.xinzhe.projectmentor.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pm_project")
public class Project {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String name;

    private String githubUrl;

    private String description;

    private String projectType;

    private String techStack;

    /**
     * PENDING：待分析
     * ANALYZING：分析中
     * FINISHED：分析完成
     * FAILED：分析失败
     */
    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}