ALTER TABLE pm_user
    ADD CONSTRAINT uk_user_email UNIQUE (email);

ALTER TABLE pm_project_file
    ADD CONSTRAINT uk_project_file_path UNIQUE (project_id, file_path);

ALTER TABLE pm_user_plan
    ALTER COLUMN remaining_credits SET DEFAULT 10;

ALTER TABLE pm_project_qa_record
    ADD INDEX idx_qa_project_id (project_id);
