package com.xinzhe.projectmentor.qa.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ProjectQaHistoryVO {

    private Long id;

    private String question;

    private String answer;

    private Boolean aiUsed;

    private List<ProjectQaEvidenceVO> evidences;

    private List<String> suggestedFollowUps;

    private LocalDateTime createTime;
}
