-- ============================================================
--  V13: MEDIUM PRIORITY SCHEMA FIXES
-- ============================================================

-- ============================================================
--  V3-A-2: Standardize audit column precision to DATETIME(6)
--  Applies to workflows (V3) and V5 tables that use plain DATETIME
-- ============================================================

-- workflows table (V3)
ALTER TABLE workflows
    MODIFY COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6);

-- job_workflow_step_activities (V5) — has created_at only
ALTER TABLE job_workflow_step_activities
    MODIFY COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);

-- job_workflow_step_attachments (V5) — has created_at, updated_at
ALTER TABLE job_workflow_step_attachments
    MODIFY COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6);

-- job_workflow_step_comments (V5) — has created_at, updated_at
ALTER TABLE job_workflow_step_comments
    MODIFY COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6);

-- job_workflow_step_visit_logs (V5) — has created_at, updated_at
ALTER TABLE job_workflow_step_visit_logs
    MODIFY COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6);

-- job_workflows (V5) — started_at and completed_at are event timestamps, not audit cols
-- standardize for consistency with the rest of the schema
ALTER TABLE job_workflows
    MODIFY COLUMN started_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    MODIFY COLUMN completed_at DATETIME(6) NULL;

-- ============================================================
--  V3-A-4: Unique constraint on (workflow_id, order_index)
-- ============================================================
ALTER TABLE workflow_steps
    ADD CONSTRAINT uk_workflow_steps_order UNIQUE (workflow_id, order_index);

-- ============================================================
--  V5-A-3: Add ON DELETE CASCADE to fk_visit_log_step
--  All other child tables in V5 already have CASCADE; this one
--  was created with the default RESTRICT.
-- ============================================================
ALTER TABLE job_workflow_step_visit_logs
    DROP FOREIGN KEY fk_visit_log_step,
    ADD CONSTRAINT fk_visit_log_step
        FOREIGN KEY (job_workflow_step_id)
        REFERENCES job_workflow_steps(id)
        ON DELETE CASCADE;

-- ============================================================
--  V6-A-1: Make assets.created_at / updated_at NOT NULL
-- ============================================================
UPDATE assets SET created_at = NOW() WHERE created_at IS NULL;
UPDATE assets SET updated_at = NOW() WHERE updated_at IS NULL;

ALTER TABLE assets
    MODIFY COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    MODIFY COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- ============================================================
--  V6-A-2: Unique constraint on (company_id, serial_number)
--  NULL serial_numbers are intentionally allowed (MySQL treats
--  each NULL as distinct in a unique index).
-- ============================================================
ALTER TABLE assets
    ADD CONSTRAINT uq_assets_company_serial UNIQUE (company_id, serial_number);

-- ============================================================
--  V6-A-3: Index on asset_job_assignments.assigned_worker_id
--  V11 does NOT add this index — safe to add here.
-- ============================================================
ALTER TABLE asset_job_assignments
    ADD INDEX idx_assignment_worker (assigned_worker_id);

-- ============================================================
--  V5-A-4: Index on job_workflow_step_workers.worker_id
--  SKIPPED — V11 already adds idx_jwsw_worker_id on this column.
-- ============================================================
