-- ============================================
--  ESTIMATE DOCUMENTS
-- ============================================
CREATE TABLE estimate_documents (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    estimate_id     BIGINT        NOT NULL,
    company_id      BIGINT        NOT NULL,
    document_number VARCHAR(50)   NOT NULL,
    s3_key          VARCHAR(500)  NOT NULL,
    total_net       DECIMAL(10,2) NOT NULL,
    total_vat       DECIMAL(10,2) NOT NULL,
    grand_total     DECIMAL(10,2) NOT NULL,
    valid_until     DATE          NULL,
    reference       VARCHAR(100)  NULL,
    notes           VARCHAR(500)  NULL,
    created_at      DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)            DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_est_doc_estimate FOREIGN KEY (estimate_id) REFERENCES estimates(id) ON DELETE RESTRICT,
    CONSTRAINT fk_est_doc_company  FOREIGN KEY (company_id)  REFERENCES companies(id) ON DELETE CASCADE,
    UNIQUE INDEX uq_est_doc_number (document_number),
    INDEX idx_est_doc_estimate_company (estimate_id, company_id),
    INDEX idx_est_doc_company (company_id)
) ENGINE=InnoDB;

-- ============================================
--  JOB LINE ITEM SNAPSHOTS (merged table)
--  Replaces: invoice_line_item_snapshots
--  Also covers: estimate_document_line_item_snapshots (new)
-- ============================================
CREATE TABLE job_line_item_snapshots (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    type                VARCHAR(20)   NOT NULL,
    invoice_id          BIGINT        NULL,
    estimate_document_id BIGINT       NULL,
    source_line_item_id BIGINT        NOT NULL,
    product_code        VARCHAR(50)   NOT NULL,
    product_description VARCHAR(255)  NOT NULL,
    additional_details  VARCHAR(500)  NULL,
    unit_price          DECIMAL(10,2) NOT NULL,
    quantity            DECIMAL(10,4) NOT NULL,
    vat_rate            DECIMAL(5,2)  NOT NULL,
    net_amount          DECIMAL(10,2) NOT NULL,
    vat_amount          DECIMAL(10,2) NOT NULL,
    total_amount        DECIMAL(10,2) NOT NULL,
    created_at          DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_jlis_invoice           FOREIGN KEY (invoice_id)           REFERENCES invoices(id)           ON DELETE CASCADE,
    CONSTRAINT fk_jlis_estimate_document FOREIGN KEY (estimate_document_id) REFERENCES estimate_documents(id) ON DELETE CASCADE,
    INDEX idx_jlis_invoice           (invoice_id),
    INDEX idx_jlis_estimate_document (estimate_document_id),
    INDEX idx_jlis_source            (source_line_item_id)
) ENGINE=InnoDB;

-- Migrate existing invoice snapshots into the unified table
INSERT INTO job_line_item_snapshots
    (type, invoice_id, estimate_document_id, source_line_item_id,
     product_code, product_description, additional_details,
     unit_price, quantity, vat_rate, net_amount, vat_amount, total_amount)
SELECT
    'INVOICE',
    s.invoice_id,
    NULL,
    s.source_line_item_id,
    s.product_code,
    s.product_description,
    s.additional_details,
    s.unit_price,
    s.quantity,
    s.vat_rate,
    s.net_amount,
    s.vat_amount,
    s.total_amount
FROM invoice_line_item_snapshots s;

-- Drop old snapshots table
ALTER TABLE invoice_line_item_snapshots DROP FOREIGN KEY fk_snap_invoice;
DROP TABLE invoice_line_item_snapshots;

-- ============================================
--  SEQUENCE COUNTER FOR ESTIMATE DOCUMENTS
-- ============================================
ALTER TABLE company_counters
    ADD COLUMN next_estimate_document_id BIGINT NOT NULL DEFAULT 1;
