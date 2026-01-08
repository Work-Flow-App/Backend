-- =========================
-- Workflows
-- =========================
CREATE TABLE workflows (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_workflows_company_name UNIQUE (company_id, name),
    CONSTRAINT fk_workflows_company
        FOREIGN KEY (company_id) REFERENCES companies(id)
) ENGINE=InnoDB;

-- =========================
-- Workflow Steps
-- =========================
CREATE TABLE workflow_steps (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workflow_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    order_index INT NOT NULL,
    optional BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_workflow_steps_workflow
        FOREIGN KEY (workflow_id) REFERENCES workflows(id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- =========================
-- Job Workflows
-- =========================
CREATE TABLE job_workflows (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id BIGINT NOT NULL,
    started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME NULL,
    status VARCHAR(50),
    CONSTRAINT fk_job_workflows_job
        FOREIGN KEY (job_id) REFERENCES jobs(id),
    CONSTRAINT uk_job_workflows_job UNIQUE (job_id)
) ENGINE=InnoDB;

-- =========================
-- Job Workflow Steps
-- =========================
CREATE TABLE job_workflow_steps (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_workflow_id BIGINT NOT NULL,
    name VARCHAR(255),
    description TEXT,
    order_index INT NOT NULL,
    status VARCHAR(50) NOT NULL,
    started_at DATETIME NULL,
    completed_at DATETIME NULL,
    CONSTRAINT fk_job_workflow_steps_job_workflow
        FOREIGN KEY (job_workflow_id) REFERENCES job_workflows(id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- =========================
-- Job Workflow Step Workers (Many-to-Many)
-- =========================
CREATE TABLE job_workflow_step_workers (
    job_workflow_step_id BIGINT NOT NULL,
    worker_id BIGINT NOT NULL,
    PRIMARY KEY (job_workflow_step_id, worker_id),
    CONSTRAINT fk_jwsw_step
        FOREIGN KEY (job_workflow_step_id) REFERENCES job_workflow_steps(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_jwsw_worker
        FOREIGN KEY (worker_id) REFERENCES workers(id)
) ENGINE=InnoDB;
