package com.xinzhe.projectmentor.analysis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pm_analysis_report")
public class AnalysisReport {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    private Long taskId;

    private Integer totalScore;

    private Integer runnabilityScore;

    private Integer authenticityScore;

    private Integer structureScore;

    private Integer readmeScore;

    private Integer securityScore;

    private Integer engineeringScore;

    private Integer interviewScore;

    private String summary;

    private String strengths;

    private String weaknesses;

    private String riskPoints;

    private String evidenceChain;

    private String suggestions;

    private String resumeBasic;

    private String resumeStandard;

    private String resumeAdvanced;

    private LocalDateTime createTime;
}