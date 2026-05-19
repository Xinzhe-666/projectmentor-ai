CREATE DATABASE IF NOT EXISTS projectmentor_ai
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE projectmentor_ai;

CREATE TABLE IF NOT EXISTS pm_user
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username    VARCHAR(50)  NOT NULL UNIQUE COMMENT '用户名',
    password    VARCHAR(255) NOT NULL COMMENT 'BCrypt加密后的密码',
    email       VARCHAR(100) NOT NULL COMMENT '邮箱',
    role        VARCHAR(20) DEFAULT 'USER' COMMENT '角色：USER/ADMIN',
    status      TINYINT     DEFAULT 1 COMMENT '状态：1正常，0禁用',
    create_time DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户表';

CREATE TABLE IF NOT EXISTS pm_user_plan
(
    id                BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id           BIGINT   NOT NULL COMMENT '用户ID',
    plan_type         VARCHAR(30) DEFAULT 'FREE' COMMENT '套餐类型',
    remaining_credits INT         DEFAULT 3 COMMENT '剩余额度',
    expire_time       DATETIME NULL COMMENT '过期时间',
    create_time       DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_id (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户套餐与额度表';

CREATE TABLE IF NOT EXISTS pm_credit_log
(
    id             BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id        BIGINT       NOT NULL COMMENT '用户ID',
    change_amount  INT          NOT NULL COMMENT '额度变化数量，正数增加，负数扣减',
    before_amount  INT          NOT NULL COMMENT '变化前额度',
    after_amount   INT          NOT NULL COMMENT '变化后额度',
    operation_type VARCHAR(50)  NOT NULL COMMENT '操作类型',
    business_id    BIGINT       NULL COMMENT '关联业务ID',
    remark         VARCHAR(255) NULL COMMENT '备注',
    create_time    DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='额度流水表';

CREATE TABLE IF NOT EXISTS pm_project
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '项目ID',
    user_id      BIGINT       NOT NULL COMMENT '用户ID',
    name         VARCHAR(100) NOT NULL COMMENT '项目名称',
    github_url   VARCHAR(255) NULL COMMENT 'GitHub地址',
    description  TEXT         NULL COMMENT '项目描述',
    project_type VARCHAR(50)  NULL COMMENT '项目类型，例如 BACKEND/FRONTEND/AI/FULLSTACK',
    tech_stack   VARCHAR(255) NULL COMMENT '技术栈标签，逗号分隔',
    status       VARCHAR(30) DEFAULT 'PENDING' COMMENT '分析状态：PENDING/ANALYZING/FINISHED/FAILED',
    create_time  DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='项目表';

CREATE TABLE IF NOT EXISTS pm_project_file
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '文件ID',
    project_id  BIGINT       NOT NULL COMMENT '项目ID',
    file_path   VARCHAR(255) NOT NULL COMMENT '文件路径，例如 README.md / src/main/java/xxx/UserController.java',
    file_type   VARCHAR(50)  NOT NULL COMMENT '文件类型，例如 README/POM/YML/JAVA/SQL/DOCKER/OTHER',
    content     LONGTEXT     NOT NULL COMMENT '文件内容',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_project_id (project_id),
    INDEX idx_file_type (file_type)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='项目文件表';

CREATE TABLE IF NOT EXISTS pm_analysis_report
(
    id                 BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '报告ID',
    project_id         BIGINT NOT NULL COMMENT '项目ID',
    task_id            BIGINT NULL COMMENT '任务ID，异步任务阶段使用',
    total_score        INT COMMENT '总评分',
    runnability_score  INT COMMENT '可运行性评分',
    authenticity_score INT COMMENT '项目真实性评分',
    structure_score    INT COMMENT '代码结构评分',
    readme_score       INT COMMENT 'README可信度评分',
    security_score     INT COMMENT '安全性评分',
    engineering_score  INT COMMENT '工程化程度评分',
    interview_score    INT COMMENT '面试价值评分',
    summary            TEXT COMMENT '项目一句话总结',
    strengths          TEXT COMMENT '项目优点',
    weaknesses         TEXT COMMENT '项目不足',
    risk_points        LONGTEXT COMMENT '风险点JSON',
    evidence_chain     LONGTEXT COMMENT '证据链JSON',
    suggestions        LONGTEXT COMMENT '建议JSON',
    resume_basic       TEXT COMMENT '简历保守版',
    resume_standard    TEXT COMMENT '简历标准版',
    resume_advanced    TEXT COMMENT '简历冲刺版',
    create_time        DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_project_id (project_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='项目审计报告表';
CREATE TABLE IF NOT EXISTS pm_interview_session (
                                                    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '面试会话ID',
                                                    user_id BIGINT NOT NULL COMMENT '用户ID',
                                                    project_id BIGINT NOT NULL COMMENT '项目ID',
                                                    mode VARCHAR(50) NOT NULL COMMENT '面试模式',
                                                    status VARCHAR(30) DEFAULT 'RUNNING' COMMENT '状态：RUNNING/FINISHED',
                                                    total_score INT NULL COMMENT '总评分',
                                                    summary TEXT NULL COMMENT '面试总结',
                                                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                                    finish_time DATETIME NULL COMMENT '结束时间',
                                                    INDEX idx_user_id (user_id),
                                                    INDEX idx_project_id (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试会话表';

CREATE TABLE IF NOT EXISTS pm_interview_message (
                                                    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '面试消息ID',
                                                    session_id BIGINT NOT NULL COMMENT '会话ID',
                                                    role VARCHAR(20) NOT NULL COMMENT '角色：INTERVIEWER/USER/SYSTEM',
                                                    content TEXT NOT NULL COMMENT '消息内容',
                                                    score INT NULL COMMENT '用户回答评分',
                                                    feedback TEXT NULL COMMENT '反馈',
                                                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                                    INDEX idx_session_id (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试消息表';