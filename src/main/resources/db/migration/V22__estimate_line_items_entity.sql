-- Drop the old ManyToMany join table
ALTER TABLE estimate_line_items DROP FOREIGN KEY FK_eli_estimate;
ALTER TABLE estimate_line_items DROP FOREIGN KEY fk_eli_line_item;
DROP TABLE estimate_line_items;

-- Create the new EstimateLineItem entity table (job-scoped isolated line item copy)
CREATE TABLE estimate_line_items (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    estimate_id         BIGINT        NOT NULL,
    status              ENUM('AVAILABLE','WAITING_APPROVAL','APPROVED','INVOICED')
                                      NOT NULL DEFAULT 'AVAILABLE',
    source_line_item_id BIGINT        NULL,
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
    updated_at          DATETIME(6)            DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_eli_estimate FOREIGN KEY (estimate_id) REFERENCES estimates(id) ON DELETE CASCADE,
    INDEX idx_eli_estimate        (estimate_id),
    INDEX idx_eli_status          (status),
    INDEX idx_eli_source          (source_line_item_id)
) ENGINE=InnoDB;
