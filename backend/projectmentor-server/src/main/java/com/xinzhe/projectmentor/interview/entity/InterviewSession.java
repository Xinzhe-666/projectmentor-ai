package com.xinzhe.projectmentor.interview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pm_interview_session")
public class InterviewSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long projectId;

    private String mode;

    private String status;

    private Integer totalScore;

    private String summary;

    private LocalDateTime createTime;

    private LocalDateTime finishTime;
}