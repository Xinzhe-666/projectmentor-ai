package com.xinzhe.projectmentor.interview.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class InterviewSessionVO {

    private Long id;

    private Long projectId;

    private String projectName;

    private String mode;

    private String status;

    private Integer totalScore;

    private String summary;

    private LocalDateTime createTime;

    private LocalDateTime finishTime;

    private List<InterviewMessageVO> messages;
}