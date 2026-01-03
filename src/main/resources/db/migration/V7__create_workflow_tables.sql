-- ============================================
--   CREATE WORKFLOW-RELATED TABLES (MySQL)
-- ============================================

-- --------------------------------------------
-- WORKFLOWS
-- --------------------------------------------
CREATE TABLE workflows (
    id BIGINT NOT NULL AUTO_INCREMENT,
    company_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_workflows PRIMARY KEY (id),
    CONSTRAINT uk_workflows_company_name UNIQUE (company_id, name),
    CONSTRAINT fk_workflows_company
        FOREIGN KEY (company_id)
        REFERENCES companies (id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- --------------------------------------------
-- WORKFLOW STEPS
-- --------------------------------------------
CREATE TABLE workflow_steps (
    id BIGINT NOT NULL AUTO_INCREMENT,
    workflow_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    order_index INT NOT NULL,
    optional TINYINT(1) NOT NULL DEFAULT 0,

    CONSTRAINT pk_workflow_steps PRIMARY KEY (id),
    CONSTRAINT fk_workflow_steps_workflow
        FOREIGN KEY (workflow_id)
        REFERENCES workflows (id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- --------------------------------------------
-- JOB WORKFLOWS
-- --------------------------------------------
CREATE TABLE job_workflows (
    id BIGINT NOT NULL AUTO_INCREMENT,
    job_id BIGINT NOT NULL,
    workflow_id BIGINT NOT NULL,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,

    CONSTRAINT pk_job_workflows PRIMARY KEY (id),
    CONSTRAINT uk_job_workflows_job UNIQUE (job_id),
    CONSTRAINT fk_job_workflows_job
        FOREIGN KEY (job_id)
        REFERENCES jobs (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_job_workflows_workflow
        FOREIGN KEY (workflow_id)
        REFERENCES workflows (id)
        ON DELETE RESTRICT
) ENGINE=InnoDB;

-- --------------------------------------------
-- JOB WORKFLOW STEPS
-- --------------------------------------------
CREATE TABLE job_workflow_steps (
    id BIGINT NOT NULL AUTO_INCREMENT,
    job_workflow_id BIGINT NOT NULL,
    workflow_step_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,

    CONSTRAINT pk_job_workflow_steps PRIMARY KEY (id),
    CONSTRAINT fk_job_workflow_steps_job_workflow
        FOREIGN KEY (job_workflow_id)
        REFERENCES job_workflows (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_job_workflow_steps_workflow_step
        FOREIGN KEY (workflow_step_id)
        REFERENCES workflow_steps (id)
        ON DELETE RESTRICT
) ENGINE=InnoDB;

-- --------------------------------------------
-- INDEXES
-- --------------------------------------------
CREATE INDEX idx_workflow_steps_workflow_id
    ON workflow_steps (workflow_id);

CREATE INDEX idx_job_workflow_steps_job_workflow_id
    ON job_workflow_steps (job_workflow_id);

CREATE INDEX idx_job_workflow_steps_status
    ON job_workflow_steps (status);
