CREATE TABLE pm_project_vector_index
(
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id          BIGINT       NOT NULL,
    user_id             BIGINT       NOT NULL,
    status              VARCHAR(24)  NOT NULL DEFAULT 'NOT_INDEXED',
    active_generation   BIGINT       NULL,
    active_build_id     VARCHAR(64)  NULL,
    pending_generation  BIGINT       NULL,
    pending_build_id    VARCHAR(64)  NULL,
    source_revision     BIGINT       NOT NULL DEFAULT 0,
    reschedule_required TINYINT(1)   NOT NULL DEFAULT 0,
    collection_name     VARCHAR(128) NOT NULL DEFAULT 'pmai_code_chunks_v1',
    embedding_provider  VARCHAR(64)  NULL,
    embedding_model     VARCHAR(128) NULL,
    embedding_dimension INT          NULL,
    chunk_count         INT          NOT NULL DEFAULT 0,
    indexed_file_count  INT          NOT NULL DEFAULT 0,
    last_error_code     VARCHAR(64)  NULL,
    last_error_message  VARCHAR(512) NULL,
    last_indexed_at     DATETIME(6)  NULL,
    created_at          DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_vector_index_project (project_id),
    INDEX idx_vector_index_owner (user_id, project_id),
    INDEX idx_vector_index_status (status, updated_at),
    CONSTRAINT chk_vector_index_status CHECK (status IN
        ('NOT_INDEXED', 'PENDING', 'RUNNING', 'READY', 'DEGRADED', 'FAILED', 'DELETING')),
    CONSTRAINT chk_vector_index_generations CHECK
        (pending_generation IS NULL OR active_generation IS NULL OR pending_generation <> active_generation),
    CONSTRAINT chk_vector_index_active_build CHECK
        ((active_generation IS NULL AND active_build_id IS NULL) OR
         (active_generation IS NOT NULL AND active_build_id IS NOT NULL)),
    CONSTRAINT chk_vector_index_pending_build CHECK
        ((pending_generation IS NULL AND pending_build_id IS NULL) OR pending_generation IS NOT NULL)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '项目向量索引事实状态';

CREATE TABLE pm_vector_index_task
(
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id        BIGINT       NOT NULL,
    user_id           BIGINT       NOT NULL,
    task_type         VARCHAR(24)  NOT NULL DEFAULT 'REBUILD',
    active_key        VARCHAR(128) NULL,
    correlation_id    VARCHAR(64)  NOT NULL,
    generation        BIGINT       NOT NULL,
    target_revision   BIGINT       NOT NULL,
    build_id          VARCHAR(64)  NULL,
    status            VARCHAR(24)  NOT NULL DEFAULT 'PENDING',
    progress          INT          NOT NULL DEFAULT 0,
    worker_id         VARCHAR(128) NULL,
    lease_expires_at  DATETIME(6)  NULL,
    heartbeat_at      DATETIME(6)  NULL,
    execution_attempt INT          NOT NULL DEFAULT 0,
    execution_version BIGINT       NOT NULL DEFAULT 0,
    last_message_id   VARCHAR(64)  NULL,
    retry_reason      VARCHAR(512) NULL,
    error_code        VARCHAR(64)  NULL,
    error_message     VARCHAR(512) NULL,
    created_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    completed_at      DATETIME(6)  NULL,
    UNIQUE KEY uk_vector_task_active (active_key),
    UNIQUE KEY uk_vector_task_correlation (correlation_id),
    UNIQUE KEY uk_vector_task_generation (project_id, generation),
    INDEX idx_vector_task_owner (user_id, project_id, id),
    INDEX idx_vector_task_lease (status, lease_expires_at, id),
    CONSTRAINT chk_vector_task_status CHECK (status IN
        ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED', 'CANCELLED', 'SUPERSEDED')),
    CONSTRAINT chk_vector_task_progress CHECK (progress BETWEEN 0 AND 100)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '可靠项目向量索引任务';

CREATE TABLE pm_vector_file_manifest
(
    id                 BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id         BIGINT       NOT NULL,
    user_id            BIGINT       NOT NULL,
    file_id            BIGINT       NULL,
    file_path          VARCHAR(255) NOT NULL,
    content_hash       CHAR(64)     NOT NULL,
    generation         BIGINT       NOT NULL,
    build_id           VARCHAR(64)  NOT NULL,
    embedding_model    VARCHAR(128) NOT NULL,
    chunk_count        INT          NOT NULL,
    indexed_at         DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_vector_manifest_file (project_id, generation, build_id, file_path),
    INDEX idx_vector_manifest_generation (user_id, project_id, generation, build_id),
    INDEX idx_vector_manifest_hash (project_id, content_hash)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '项目向量文件索引清单';

CREATE TABLE pm_vector_index_outbox
(
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id          VARCHAR(64)  NOT NULL,
    task_id           BIGINT       NOT NULL,
    event_type        VARCHAR(50)  NOT NULL,
    schema_version    INT          NOT NULL,
    payload           JSON         NOT NULL,
    exchange_name     VARCHAR(128) NOT NULL,
    routing_key       VARCHAR(128) NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'NEW',
    publish_attempt   INT          NOT NULL DEFAULT 0,
    next_attempt_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    claim_owner       VARCHAR(128) NULL,
    claim_expires_at  DATETIME(6)  NULL,
    last_error        VARCHAR(512) NULL,
    created_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    published_at      DATETIME(6)  NULL,
    UNIQUE KEY uk_vector_outbox_event (event_id),
    INDEX idx_vector_outbox_relay (status, next_attempt_at, claim_expires_at, id),
    INDEX idx_vector_outbox_task (task_id, id),
    CONSTRAINT fk_vector_outbox_task FOREIGN KEY (task_id)
        REFERENCES pm_vector_index_task (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '项目索引事务 Outbox';
