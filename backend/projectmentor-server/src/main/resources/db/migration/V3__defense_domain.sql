CREATE TABLE IF NOT EXISTS pm_defense_session
(
    id             BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Defense session ID',
    project_id     BIGINT      NOT NULL COMMENT 'Project ID',
    report_id      BIGINT      NOT NULL COMMENT 'Bound analysis report ID',
    mode           VARCHAR(50) NOT NULL COMMENT 'Defense mode',
    status         VARCHAR(30) NOT NULL DEFAULT 'CREATING' COMMENT 'CREATING/ACTIVE/INSUFFICIENT_DATA/COMPLETED',
    question_count INT         NOT NULL DEFAULT 0 COMMENT 'Generated question count',
    created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    updated_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update time',
    INDEX idx_defense_session_project (project_id),
    INDEX idx_defense_session_report (report_id),
    INDEX idx_defense_session_status (status),
    CONSTRAINT fk_defense_session_project FOREIGN KEY (project_id) REFERENCES pm_project (id) ON DELETE CASCADE,
    CONSTRAINT fk_defense_session_report FOREIGN KEY (report_id) REFERENCES pm_analysis_report (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Project defense sessions';

CREATE TABLE IF NOT EXISTS pm_defense_question
(
    id               BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Defense question ID',
    session_id       BIGINT       NOT NULL COMMENT 'Defense session ID',
    question         TEXT         NOT NULL COMMENT 'Question text',
    category         VARCHAR(50)  NOT NULL COMMENT 'Question category',
    related_claims   LONGTEXT     NULL COMMENT 'Related claim snapshots as JSON',
    related_evidence LONGTEXT     NULL COMMENT 'Related evidence snapshots as JSON',
    sort_order       INT          NOT NULL COMMENT 'Question order within a session',
    UNIQUE KEY uk_defense_question_order (session_id, sort_order),
    INDEX idx_defense_question_session (session_id),
    CONSTRAINT fk_defense_question_session FOREIGN KEY (session_id) REFERENCES pm_defense_session (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Rule-generated project defense questions';

CREATE TABLE IF NOT EXISTS pm_defense_answer
(
    id                BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Defense answer ID',
    question_id       BIGINT       NOT NULL COMMENT 'Defense question ID',
    answer_text       TEXT         NOT NULL COMMENT 'User answer text',
    evaluation_status VARCHAR(30)  NOT NULL COMMENT 'SUPPORTED/PARTIAL/INSUFFICIENT',
    review_result     LONGTEXT     NOT NULL COMMENT 'Rule review result as JSON',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Submission time',
    UNIQUE KEY uk_defense_answer_question (question_id),
    INDEX idx_defense_answer_status (evaluation_status),
    CONSTRAINT fk_defense_answer_question FOREIGN KEY (question_id) REFERENCES pm_defense_question (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Project defense answers and rule reviews';
