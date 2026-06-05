package com.xinzhe.projectmentor.interview.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InterviewSessionListItemVO {

    private Long sessionId;

    private Long projectId;

    private String projectName;

    private Integer totalScore;

    private Integer questionCount;

    private Integer answeredCount;

    private Integer skippedCount;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
