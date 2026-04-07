-- ============================================
--  JOB WORKFLOWS
-- ============================================
CREATE TABLE job_workflows (
    id           BIGINT   AUTO_INCREMENT PRIMARY KEY,
    job_id       BIGINT   NOT NULL,
    started_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    completed_at DATETIME(6) NULL,
    status       VARCHAR(50) NOT NULL DEFAULT 'NOT_STARTED',
    CONSTRAINT fk_job_workflows_job FOREIGN KEY (job_id) REFERENCES jobs(id),
    CONSTRAINT uk_job_workflows_job UNIQUE (job_id)
) ENGINE=InnoDB;

-- ============================================
--  JOB WORKFLOW STEPS
-- ============================================
CREATE TABLE job_workflow_steps (
    id              BIGINT        AUTO_INCREMENT PRIMARY KEY,
    job_workflow_id BIGINT        NOT NULL,
    name            VARCHAR(255)  NOT NULL,
    description     TEXT,
    order_index     INT           NOT NULL,
    status          VARCHAR(50)   NOT NULL,
    started_at      DATETIME      NULL,
    completed_at    DATETIME      NULL,
    CONSTRAINT fk_job_workflow_steps_job_workflow FOREIGN KEY (job_workflow_id) REFERENCES job_workflows(id) ON DELETE CASCADE,
    INDEX idx_jws_workflow_order (job_workflow_id, order_index)
) ENGINE=InnoDB;

-- ============================================
--  JOB WORKFLOW STEP WORKERS
-- ============================================
CREATE TABLE job_workflow_step_workers (
    job_workflow_step_id BIGINT NOT NULL,
    worker_id            BIGINT NOT NULL,
    PRIMARY KEY (job_workflow_step_id, worker_id),
    CONSTRAINT fk_jwsw_step   FOREIGN KEY (job_workflow_step_id) REFERENCES job_workflow_steps(id) ON DELETE CASCADE,
    CONSTRAINT fk_jwsw_worker FOREIGN KEY (worker_id)            REFERENCES workers(id),
    INDEX idx_jwsw_worker_id (worker_id)
) ENGINE=InnoDB;

-- ============================================
--  JOB WORKFLOW STEP ACTIVITIES
-- ============================================
CREATE TABLE job_workflow_step_activities (
    id                   BIGINT      NOT NULL AUTO_INCREMENT,
    job_workflow_step_id BIGINT      NOT NULL,
    actor_id             BIGINT      NOT NULL,
    type                 VARCHAR(100) NOT NULL,
    message              TEXT,
    created_at           DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_jwsa_step  FOREIGN KEY (job_workflow_step_id) REFERENCES job_workflow_steps(id) ON DELETE CASCADE,
    CONSTRAINT fk_jwsa_actor FOREIGN KEY (actor_id)             REFERENCES users(id) ON DELETE RESTRICT,
    INDEX idx_jwsa_step_id  (job_workflow_step_id),
    INDEX idx_jwsa_actor_id (actor_id)
) ENGINE=InnoDB;

-- ============================================
--  JOB WORKFLOW STEP ATTACHMENTS
-- ============================================
CREATE TABLE job_workflow_step_attachments (
    id                   BIGINT        NOT NULL AUTO_INCREMENT,
    job_workflow_step_id BIGINT        NOT NULL,
    uploaded_by          BIGINT        NOT NULL,
    file_name            VARCHAR(255)  NOT NULL,
    file_type            VARCHAR(100),
    file_url             VARCHAR(1024) NOT NULL,
    type                 VARCHAR(50)   NOT NULL DEFAULT 'GENERAL',
    description          TEXT          NULL,
    created_at           DATETIME(6),
    updated_at           DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_jwsa_attach_step FOREIGN KEY (job_workflow_step_id) REFERENCES job_workflow_steps(id) ON DELETE CASCADE,
    CONSTRAINT fk_jwsa_attach_user FOREIGN KEY (uploaded_by)          REFERENCES users(id) ON DELETE RESTRICT,
    INDEX idx_jws_attach_step_id    (job_workflow_step_id),
    INDEX idx_jws_attach_uploaded_by (uploaded_by),
    INDEX idx_step_attachments_type  (type)
) ENGINE=InnoDB;

-- ============================================
--  JOB WORKFLOW STEP COMMENTS
-- ============================================
CREATE TABLE job_workflow_step_comments (
    id                   BIGINT      NOT NULL AUTO_INCREMENT,
    job_workflow_step_id BIGINT      NOT NULL,
    author_id            BIGINT      NOT NULL,
    content              TEXT        NOT NULL,
    type                 VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
    created_at           DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at           DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_jwsc_step   FOREIGN KEY (job_workflow_step_id) REFERENCES job_workflow_steps(id) ON DELETE CASCADE,
    CONSTRAINT fk_jwsc_author FOREIGN KEY (author_id)            REFERENCES users(id) ON DELETE RESTRICT,
    INDEX idx_jwsc_step_id  (job_workflow_step_id),
    INDEX idx_jwsc_author_id (author_id),
    INDEX idx_step_comments_type (type)
) ENGINE=InnoDB;

-- ============================================
--  JOB WORKFLOW STEP VISIT LOGS
-- ============================================
CREATE TABLE job_workflow_step_visit_logs (
    id                   BIGINT   AUTO_INCREMENT PRIMARY KEY,
    job_workflow_step_id BIGINT   NOT NULL,
    logged_by_id         BIGINT   NOT NULL,
    visit_date           DATE     NOT NULL,
    time_in              TIME     NOT NULL,
    time_out             TIME     DEFAULT NULL,
    description          TEXT,
    created_at           DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at           DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_visit_log_step FOREIGN KEY (job_workflow_step_id) REFERENCES job_workflow_steps(id) ON DELETE CASCADE,
    CONSTRAINT fk_visit_log_user FOREIGN KEY (logged_by_id)         REFERENCES users(id),
    INDEX idx_visit_log_step      (job_workflow_step_id),
    INDEX idx_visit_log_date_time (visit_date, time_in)
) ENGINE=InnoDB;
