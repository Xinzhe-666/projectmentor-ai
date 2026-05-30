package com.xinzhe.projectmentor.qa.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pm_project_qa_record")
public class ProjectQaRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long projectId;

    private String question;

    private String answer;

    private Integer aiUsed;

    private String evidenceJson;

    private String suggestedFollowUpsJson;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
