-- ============================================
--   UPDATE JOB STATUS TO ENUM
-- ============================================
-- This migration converts the jobs.status column from VARCHAR to ENUM
-- to match the JobStatus enum in the application code

ALTER TABLE jobs
MODIFY COLUMN status ENUM('NEW', 'PENDING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'NEW';