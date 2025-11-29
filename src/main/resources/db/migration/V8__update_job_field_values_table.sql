-- ============================================
--   UPDATE JOB_FIELD_VALUES TO SUPPORT MULTI-TYPED VALUES
-- ============================================

ALTER TABLE job_field_values
ADD COLUMN string_value TEXT;

ALTER TABLE job_field_values
ADD COLUMN number_value DOUBLE;

ALTER TABLE job_field_values
ADD COLUMN boolean_value BOOLEAN;

ALTER TABLE job_field_values
ADD COLUMN date_value TIMESTAMP;

ALTER TABLE job_field_values
ADD COLUMN json_value JSON;

ALTER TABLE job_field_values
ADD COLUMN reference_id BIGINT;

ALTER TABLE job_field_values
ADD COLUMN reference_type VARCHAR(150);

-- Optional: if you want to drop the old 'value' column
-- ALTER TABLE job_field_values
-- DROP COLUMN value;
