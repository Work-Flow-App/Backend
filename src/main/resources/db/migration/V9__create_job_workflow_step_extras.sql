-- =========================================================
-- Job Workflow Step Activities
-- =========================================================
CREATE TABLE job_workflow_step_activities (
    id BIGINT NOT NULL AUTO_INCREMENT,
    job_workflow_step_id BIGINT NOT NULL,
    actor_id BIGINT NOT NULL,
    type VARCHAR(100) NOT NULL,
    message TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_jwsa_step
        FOREIGN KEY (job_workflow_step_id)
        REFERENCES job_workflow_steps (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_jwsa_actor
        FOREIGN KEY (actor_id)
        REFERENCES users (id)
        ON DELETE RESTRICT
);

CREATE INDEX idx_jwsa_step_id
    ON job_workflow_step_activities (job_workflow_step_id);

CREATE INDEX idx_jwsa_actor_id
    ON job_workflow_step_activities (actor_id);


-- =========================================================
-- Job Workflow Step Attachments
-- =========================================================
CREATE TABLE job_workflow_step_attachments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    job_workflow_step_id BIGINT NOT NULL,
    uploaded_by BIGINT NOT NULL,
    file_name VARCHAR(255),
    file_type VARCHAR(100),
    file_url VARCHAR(1024),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_jwsa_attach_step
        FOREIGN KEY (job_workflow_step_id)
        REFERENCES job_workflow_steps (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_jwsa_attach_user
        FOREIGN KEY (uploaded_by)
        REFERENCES users (id)
        ON DELETE RESTRICT
);

CREATE INDEX idx_jws_attach_step_id
    ON job_workflow_step_attachments (job_workflow_step_id);

CREATE INDEX idx_jws_attach_uploaded_by
    ON job_workflow_step_attachments (uploaded_by);


-- =========================================================
-- Job Workflow Step Comments
-- =========================================================
CREATE TABLE job_workflow_step_comments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    job_workflow_step_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_jwsc_step
        FOREIGN KEY (job_workflow_step_id)
        REFERENCES job_workflow_steps (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_jwsc_author
        FOREIGN KEY (author_id)
        REFERENCES users (id)
        ON DELETE RESTRICT
);

CREATE INDEX idx_jwsc_step_id
    ON job_workflow_step_comments (job_workflow_step_id);

CREATE INDEX idx_jwsc_author_id
    ON job_workflow_step_comments (author_id);
