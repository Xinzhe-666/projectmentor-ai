ALTER TABLE pm_analysis_task
    ADD COLUMN active_key VARCHAR(100) NULL COMMENT '活动任务唯一键，终态时清空' AFTER task_type,
    ADD UNIQUE KEY uk_analysis_task_active_key (active_key);
