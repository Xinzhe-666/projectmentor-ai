package com.xinzhe.projectmentor.indexing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pm_vector_index_outbox")
public class VectorIndexOutboxEvent {
    @TableId(type = IdType.AUTO) private Long id;
    private String eventId;
    private Long taskId;
    private String eventType;
    private Integer schemaVersion;
    private String payload;
    private String exchangeName;
    private String routingKey;
    private String status;
    private Integer publishAttempt;
    private LocalDateTime nextAttemptAt;
    private String claimOwner;
    private LocalDateTime claimExpiresAt;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
}
