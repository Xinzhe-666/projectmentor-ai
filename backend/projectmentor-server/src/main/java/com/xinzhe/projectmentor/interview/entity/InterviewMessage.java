package com.xinzhe.projectmentor.interview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pm_interview_message")
public class InterviewMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;

    /**
     * INTERVIEWER / USER / SYSTEM
     */
    private String role;

    private String content;

    private Integer score;

    private String feedback;

    private LocalDateTime createTime;
}