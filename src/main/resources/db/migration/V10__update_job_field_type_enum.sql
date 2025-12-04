-- ============================================
-- V10 - Update job_field_type enum to support JSON and REFERENCE
-- ============================================

ALTER TABLE job_template_fields 
MODIFY COLUMN job_field_type 
ENUM('TEXT','NUMBER','DATE','BOOLEAN','DROPDOWN','JSON','REFERENCE') NOT NULL;
