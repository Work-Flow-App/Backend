-- ============================================
-- V13 - Add customer_id to jobs table
-- ============================================

ALTER TABLE jobs
    ADD COLUMN customer_id BIGINT NOT NULL AFTER client_id,
    ADD CONSTRAINT fk_job_customer
        FOREIGN KEY (customer_id)
        REFERENCES customers(id),
    ADD INDEX idx_customer (customer_id);
