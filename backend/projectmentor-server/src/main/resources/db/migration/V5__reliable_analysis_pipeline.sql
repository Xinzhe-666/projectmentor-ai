ALTER TABLE pm_analysis_task
    ADD COLUMN correlation_id VARCHAR(64) NULL COMMENT '跨重试稳定关联ID' AFTER active_key,
    ADD COLUMN worker_id VARCHAR(128) NULL COMMENT '当前租约持有者' AFTER progress,
    ADD COLUMN lease_expires_at DATETIME(6) NULL COMMENT '执行租约到期时间' AFTER worker_id,
    ADD COLUMN heartbeat_at DATETIME(6) NULL COMMENT '最近心跳时间' AFTER lease_expires_at,
    ADD COLUMN execution_attempt INT NOT NULL DEFAULT 0 COMMENT '已领取执行次数' AFTER heartbeat_at,
    ADD COLUMN execution_version BIGINT NOT NULL DEFAULT 0 COMMENT '围栏令牌' AFTER execution_attempt,
    ADD COLUMN last_message_id VARCHAR(64) NULL COMMENT '最近投递消息ID' AFTER execution_version,
    ADD COLUMN retry_reason VARCHAR(512) NULL COMMENT '最近重试或终态原因' AFTER last_message_id,
    ADD INDEX idx_analysis_task_lease (status, lease_expires_at, id),
    ADD INDEX idx_analysis_task_correlation (correlation_id);

ALTER TABLE pm_credit_log
    ADD COLUMN idempotency_key VARCHAR(128) NULL COMMENT '业务幂等键' AFTER business_id,
    ADD UNIQUE KEY uk_credit_log_idempotency_key (idempotency_key);

ALTER TABLE pm_analysis_report
    ADD UNIQUE KEY uk_analysis_report_task_id (task_id);

CREATE TABLE pm_analysis_outbox
(
    id                BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Outbox ID',
    event_id          VARCHAR(64)  NOT NULL COMMENT '全局唯一消息ID',
    task_id           BIGINT       NOT NULL COMMENT '分析任务ID',
    event_type        VARCHAR(50)  NOT NULL COMMENT '事件类型',
    schema_version    INT          NOT NULL COMMENT '消息协议版本',
    payload           JSON         NOT NULL COMMENT '仅含任务定位与追踪字段的精简消息',
    exchange_name     VARCHAR(128) NOT NULL COMMENT '目标 exchange',
    routing_key       VARCHAR(128) NOT NULL COMMENT '目标 routing key',
    status            VARCHAR(20)  NOT NULL DEFAULT 'NEW' COMMENT 'NEW/CLAIMED/PUBLISHED/FAILED',
    publish_attempt   INT          NOT NULL DEFAULT 0 COMMENT '发布尝试次数',
    next_attempt_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '下次可领取时间',
    claim_owner       VARCHAR(128) NULL COMMENT 'Relay claim owner',
    claim_expires_at  DATETIME(6) NULL COMMENT 'Relay claim expiry',
    last_error        VARCHAR(512) NULL COMMENT '清理后的最后错误',
    created_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    published_at      DATETIME(6) NULL,
    UNIQUE KEY uk_analysis_outbox_event_id (event_id),
    INDEX idx_analysis_outbox_relay (status, next_attempt_at, claim_expires_at, id),
    INDEX idx_analysis_outbox_task (task_id, id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='分析任务事务 Outbox';
