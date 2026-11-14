package com.xinzhe.projectmentor.indexing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pm_vector_index_task")
public class VectorIndexTask {
    @TableId(type = IdType.AUTO) private Long id;
    private Long projectId;
    private Long userId;
    private String taskType;
    private String activeKey;
    private String correlationId;
    private Long generation;
    private String status;
    private Integer progress;
    private String workerId;
    private LocalDateTime leaseExpiresAt;
    private LocalDateTime heartbeatAt;
    private Integer executionAttempt;
    private Long executionVersion;
    private String lastMessageId;
    private String retryReason;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
}
