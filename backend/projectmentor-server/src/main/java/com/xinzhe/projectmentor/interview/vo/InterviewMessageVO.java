package com.xinzhe.projectmentor.interview.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InterviewMessageVO {

    private Long id;

    private String role;

    private String content;

    private Integer score;

    private String feedback;

    private String questionCategory;

    private String evidenceStrength;

    private String sourceFile;

    private String reason;

    private Integer questionIndex;

    private Boolean skipped;

    private LocalDateTime createTime;
}
