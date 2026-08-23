-- 1. Subscription tier + purchased overages
ALTER TABLE company_subscriptions
    ADD COLUMN plan_type           VARCHAR(20) NOT NULL DEFAULT 'FREE',
    ADD COLUMN extra_user_seats    INT         NOT NULL DEFAULT 0,
    ADD COLUMN extra_storage_blocks INT        NOT NULL DEFAULT 0;

-- 2. Running storage usage total per company (cumulative, no monthly reset)
ALTER TABLE companies
    ADD COLUMN storage_used_bytes BIGINT NOT NULL DEFAULT 0;

-- 3. Composite index for the monthly job-count cap query (company_id alone already indexed,
-- but the created_at range filter needs its own leading position to avoid a scan)
CREATE INDEX idx_jobs_company_created ON jobs (company_id, created_at);

-- idx_company's leftmost prefix is now fully covered by idx_jobs_company_created above,
-- so InnoDB would otherwise maintain two B-trees on every job write for no query benefit
DROP INDEX idx_company ON jobs;

-- 4. file_size_bytes so storage usage can be decremented on delete, not just incremented on upload
ALTER TABLE asset_attachments
    ADD COLUMN file_size_bytes BIGINT DEFAULT NULL;

ALTER TABLE job_workflow_step_attachments
    ADD COLUMN file_size_bytes BIGINT DEFAULT NULL;

ALTER TABLE worker_certificates
    ADD COLUMN file_size_bytes BIGINT DEFAULT NULL;

ALTER TABLE company_documents
    ADD COLUMN file_size_bytes BIGINT DEFAULT NULL;

ALTER TABLE company_post_attachments
    ADD COLUMN file_size_bytes BIGINT DEFAULT NULL;

ALTER TABLE invoices
    ADD COLUMN file_size_bytes BIGINT DEFAULT NULL;

ALTER TABLE estimate_documents
    ADD COLUMN file_size_bytes BIGINT DEFAULT NULL;

ALTER TABLE workers
    ADD COLUMN photo_size_bytes BIGINT DEFAULT NULL;