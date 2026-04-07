-- ============================================
--  COMPANY COUNTERS
-- ============================================
CREATE TABLE company_counters (
    company_id       BIGINT NOT NULL PRIMARY KEY,
    next_job_id      BIGINT NOT NULL DEFAULT 1,
    next_worker_id   BIGINT NOT NULL DEFAULT 1,
    next_customer_id BIGINT NOT NULL DEFAULT 1,
    next_client_id   BIGINT NOT NULL DEFAULT 1,
    next_asset_id    BIGINT NOT NULL DEFAULT 1,
    next_template_id BIGINT NOT NULL DEFAULT 1,
    next_workflow_id BIGINT NOT NULL DEFAULT 1,
    next_invoice_id  BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT fk_cc_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE
) ENGINE=InnoDB;
