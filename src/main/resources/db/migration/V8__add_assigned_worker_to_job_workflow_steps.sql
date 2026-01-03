-- ============================================
--   ADD ASSIGNED WORKER TO JOB WORKFLOW STEPS (MySQL)
-- ============================================

-- --------------------------------------------
-- JOB WORKFLOW STEPS - ADD ASSIGNED WORKER
-- --------------------------------------------
ALTER TABLE job_workflow_steps
    ADD COLUMN assigned_worker_id BIGINT;

-- --------------------------------------------
-- ADD FOREIGN KEY CONSTRAINT FOR ASSIGNED WORKER
-- --------------------------------------------
ALTER TABLE job_workflow_steps
    ADD CONSTRAINT fk_job_workflow_steps_assigned_worker
        FOREIGN KEY (assigned_worker_id)
        REFERENCES workers (id)
        ON DELETE SET NULL;

-- --------------------------------------------
-- ADD INDEX ON ASSIGNED WORKER ID (OPTIONAL)
-- --------------------------------------------
CREATE INDEX idx_job_workflow_steps_assigned_worker_id
    ON job_workflow_steps (assigned_worker_id);
