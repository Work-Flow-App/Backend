-- =========================================================
-- V11: migrate job_workflow_steps to multi-worker model
-- =========================================================

-- --------------------------------------------
-- 1. Drop FK constraint on assigned_worker_id
-- --------------------------------------------
ALTER TABLE job_workflow_steps
DROP FOREIGN KEY fk_job_workflow_steps_assigned_worker;

-- --------------------------------------------
-- 2. Drop index created for assigned_worker_id
-- --------------------------------------------
DROP INDEX idx_job_workflow_steps_assigned_worker_id
ON job_workflow_steps;

-- --------------------------------------------
-- 3. Drop the assigned_worker_id column
-- --------------------------------------------
ALTER TABLE job_workflow_steps
DROP COLUMN assigned_worker_id;

-- --------------------------------------------
-- 4. Create join table for multi-workers
-- --------------------------------------------
CREATE TABLE job_workflow_step_workers (
    job_workflow_step_id BIGINT NOT NULL,
    worker_id BIGINT NOT NULL,

    PRIMARY KEY (job_workflow_step_id, worker_id),

    CONSTRAINT fk_jwsw_step
        FOREIGN KEY (job_workflow_step_id)
        REFERENCES job_workflow_steps(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_jwsw_worker
        FOREIGN KEY (worker_id)
        REFERENCES workers(id)
        ON DELETE CASCADE
) ENGINE=InnoDB;
