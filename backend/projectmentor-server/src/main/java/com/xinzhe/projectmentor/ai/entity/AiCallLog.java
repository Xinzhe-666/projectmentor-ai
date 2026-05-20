package com.xinzhe.projectmentor.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pm_ai_call_log")
public class AiCallLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String module;

    private String model;

    private Integer success;

    private Integer promptChars;

    private Integer responseChars;

    private Long latencyMs;

    private String errorMessage;

    private LocalDateTime createTime;
}
