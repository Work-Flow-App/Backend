-- ============================================
--  HIGH PRIORITY SCHEMA FIXES
-- ============================================

-- V3-A-1: workflow_steps.workflow_id index — SKIPPED
-- Already covered by V11: idx_workflow_steps_workflow_order (workflow_id, order_index)

-- V5-A-1: job_workflows.status — make NOT NULL with default
-- Fill any existing NULLs before adding constraint
UPDATE job_workflows SET status = 'NOT_STARTED' WHERE status IS NULL;
ALTER TABLE job_workflows MODIFY COLUMN status VARCHAR(50) NOT NULL DEFAULT 'NOT_STARTED';

-- V5-A-2: job_workflow_steps.job_workflow_id index — SKIPPED
-- Already covered by V11: idx_jws_workflow_order (job_workflow_id, order_index)
