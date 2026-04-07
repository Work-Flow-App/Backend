-- ============================================================
--  V14: LOW PRIORITY SCHEMA FIXES
-- ============================================================

-- ============================================================
--  V5-A-5: Make file_name and file_url NOT NULL
--  in job_workflow_step_attachments.
--  Backfill defensively before applying the constraint.
-- ============================================================
UPDATE job_workflow_step_attachments SET file_name = 'unknown' WHERE file_name IS NULL;
UPDATE job_workflow_step_attachments SET file_url  = ''        WHERE file_url  IS NULL;

ALTER TABLE job_workflow_step_attachments
    MODIFY COLUMN file_name VARCHAR(255)  NOT NULL,
    MODIFY COLUMN file_url  VARCHAR(1024) NOT NULL;
