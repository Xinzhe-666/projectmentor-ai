package com.xinzhe.projectmentor.analysis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pm_analysis_task")
public class AnalysisTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long projectId;

    private String taskType;

    private Integer creditCost;

    /**
     * PENDING / RUNNING / SUCCESS / FAILED
     */
    private String status;

    private Integer progress;

    private Long reportId;

    private String failReason;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private LocalDateTime finishTime;
}