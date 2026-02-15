-- ============================================
-- V11 - Add discussion type + attachment description
-- ============================================

-- ------------------------------------------------
-- 1. COMMENTS - Add discussion type
-- ------------------------------------------------
ALTER TABLE job_workflow_step_comments
    ADD COLUMN type VARCHAR(50) NOT NULL DEFAULT 'GENERAL';

-- ------------------------------------------------
-- 2. ATTACHMENTS - Add discussion type
-- ------------------------------------------------
ALTER TABLE job_workflow_step_attachments
    ADD COLUMN type VARCHAR(50) NOT NULL DEFAULT 'GENERAL';

-- ------------------------------------------------
-- 3. ATTACHMENTS - Add description
-- ------------------------------------------------
ALTER TABLE job_workflow_step_attachments
    ADD COLUMN description TEXT NULL;

-- ------------------------------------------------
-- 4. Optional: Add indexes for filtering by type
-- ------------------------------------------------
CREATE INDEX idx_step_comments_type 
    ON job_workflow_step_comments (type);

CREATE INDEX idx_step_attachments_type 
    ON job_workflow_step_attachments (type);