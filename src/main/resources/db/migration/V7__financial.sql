-- ============================================
--  ESTIMATES
-- ============================================
CREATE TABLE estimates (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    job_id     BIGINT      NOT NULL,
    company_id BIGINT      NOT NULL,
    notes      VARCHAR(500),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6)          DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT FK_estimates_job     FOREIGN KEY (job_id)     REFERENCES jobs(id)      ON DELETE CASCADE,
    CONSTRAINT FK_estimates_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    CONSTRAINT uq_estimates_job     UNIQUE (job_id)
) ENGINE=InnoDB;

-- ============================================
--  LINE ITEMS
-- ============================================
CREATE TABLE line_items (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    company_id          BIGINT        NOT NULL,
    product_code        VARCHAR(50)   NOT NULL,
    product_description VARCHAR(255)  NOT NULL,
    additional_details  VARCHAR(500),
    unit_price          DECIMAL(10,2) NOT NULL,
    core_or_sub         VARCHAR(10)   NOT NULL,
    quantity            DECIMAL(10,4) NOT NULL,
    vat_rate            DECIMAL(5,2)  NOT NULL,
    net_amount          DECIMAL(10,2) NOT NULL,
    vat_amount          DECIMAL(10,2) NOT NULL,
    total_amount        DECIMAL(10,2) NOT NULL,
    created_at          DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)            DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT FK_line_items_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================
--  ESTIMATE LINE ITEMS
-- ============================================
CREATE TABLE estimate_line_items (
    estimate_id  BIGINT NOT NULL,
    line_item_id BIGINT NOT NULL,
    PRIMARY KEY (estimate_id, line_item_id),
    CONSTRAINT FK_eli_estimate  FOREIGN KEY (estimate_id)  REFERENCES estimates(id)  ON DELETE CASCADE,
    CONSTRAINT FK_eli_line_item FOREIGN KEY (line_item_id) REFERENCES line_items(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================
--  INVOICES
-- ============================================
CREATE TABLE invoices (
    id             BIGINT        AUTO_INCREMENT PRIMARY KEY,
    estimate_id    BIGINT        NOT NULL,
    company_id     BIGINT        NOT NULL,
    invoice_number VARCHAR(50)   NOT NULL UNIQUE,
    s3_key         VARCHAR(500)  NOT NULL,
    due_date       DATE          NULL,
    reference      VARCHAR(100)  NULL,
    total_net      DECIMAL(10,2) NOT NULL,
    total_vat      DECIMAL(10,2) NOT NULL,
    grand_total    DECIMAL(10,2) NOT NULL,
    created_at     DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at     DATETIME(6)            DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_invoices_estimate FOREIGN KEY (estimate_id) REFERENCES estimates(id) ON DELETE CASCADE,
    CONSTRAINT fk_invoices_company  FOREIGN KEY (company_id)  REFERENCES companies(id)
) ENGINE=InnoDB;

-- ============================================
--  INVOICE LINE ITEMS
-- ============================================
CREATE TABLE invoice_line_items (
    invoice_id   BIGINT NOT NULL,
    line_item_id BIGINT NOT NULL,
    PRIMARY KEY (invoice_id, line_item_id),
    CONSTRAINT fk_inv_li_invoice   FOREIGN KEY (invoice_id)   REFERENCES invoices(id)   ON DELETE CASCADE,
    CONSTRAINT fk_inv_li_line_item FOREIGN KEY (line_item_id) REFERENCES line_items(id)
) ENGINE=InnoDB;
