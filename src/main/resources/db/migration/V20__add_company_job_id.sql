CREATE TABLE company_counters
(
    company_id        BIGINT NOT NULL PRIMARY KEY,
    next_job_id       BIGINT NOT NULL DEFAULT 1,
    next_worker_id    BIGINT NOT NULL DEFAULT 1,
    next_customer_id  BIGINT NOT NULL DEFAULT 1,
    next_client_id    BIGINT NOT NULL DEFAULT 1,
    next_asset_id     BIGINT NOT NULL DEFAULT 1,
    next_template_id  BIGINT NOT NULL DEFAULT 1,
    next_workflow_id  BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT fk_cc_company FOREIGN KEY (company_id) REFERENCES companies (id) ON DELETE CASCADE
);

ALTER TABLE jobs          ADD COLUMN job_ref       BIGINT NOT NULL DEFAULT 0;
ALTER TABLE workers       ADD COLUMN worker_ref    BIGINT NOT NULL DEFAULT 0;
ALTER TABLE customers     ADD COLUMN customer_ref  BIGINT NOT NULL DEFAULT 0;
ALTER TABLE clients       ADD COLUMN client_ref    BIGINT NOT NULL DEFAULT 0;
ALTER TABLE assets        ADD COLUMN asset_ref     BIGINT NOT NULL DEFAULT 0;
ALTER TABLE job_templates ADD COLUMN template_ref  BIGINT NOT NULL DEFAULT 0;
ALTER TABLE workflows     ADD COLUMN workflow_ref  BIGINT NOT NULL DEFAULT 0;
