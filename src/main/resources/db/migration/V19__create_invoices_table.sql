CREATE TABLE invoices
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    estimate_id    BIGINT         NOT NULL,
    company_id     BIGINT         NOT NULL,
    invoice_number VARCHAR(50)    NOT NULL UNIQUE,
    s3_key         VARCHAR(500)   NOT NULL,
    total_net      DECIMAL(10, 2) NOT NULL,
    total_vat      DECIMAL(10, 2) NOT NULL,
    grand_total    DECIMAL(10, 2) NOT NULL,
    created_at     DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at     DATETIME(6)             DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_invoices_estimate FOREIGN KEY (estimate_id) REFERENCES estimates (id) ON DELETE CASCADE,
    CONSTRAINT fk_invoices_company FOREIGN KEY (company_id) REFERENCES companies (id)
);

CREATE TABLE invoice_line_items
(
    invoice_id   BIGINT NOT NULL,
    line_item_id BIGINT NOT NULL,
    PRIMARY KEY (invoice_id, line_item_id),
    CONSTRAINT fk_inv_li_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id) ON DELETE CASCADE,
    CONSTRAINT fk_inv_li_line_item FOREIGN KEY (line_item_id) REFERENCES line_items (id)
);
