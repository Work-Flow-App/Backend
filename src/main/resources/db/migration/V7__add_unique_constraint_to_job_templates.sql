-- ============================================
--   ADD UNIQUE CONSTRAINT TO JOB TEMPLATES
-- ============================================
ALTER TABLE job_templates
ADD CONSTRAINT uq_company_name UNIQUE (company_id, name);
