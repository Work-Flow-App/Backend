CREATE TABLE estimates (
    id          BIGINT NOT NULL AUTO_INCREMENT,
    job_id      BIGINT NOT NULL,
    company_id  BIGINT NOT NULL,
    notes       VARCHAR(500),
    created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)          DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT FK_estimates_job     FOREIGN KEY (job_id)     REFERENCES jobs(id)      ON DELETE CASCADE,
    CONSTRAINT FK_estimates_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    CONSTRAINT uq_estimates_job     UNIQUE (job_id)
) ENGINE=InnoDB;

CREATE TABLE line_items (
    id                  BIGINT         NOT NULL AUTO_INCREMENT,
    company_id          BIGINT         NOT NULL,
    product_code        VARCHAR(50)    NOT NULL,
    product_description VARCHAR(255)   NOT NULL,
    additional_details  VARCHAR(500),
    unit_price          DECIMAL(10,2)  NOT NULL,
    core_or_sub         VARCHAR(10)    NOT NULL,
    quantity            DECIMAL(10,4)  NOT NULL,
    vat_rate            DECIMAL(5,4)   NOT NULL,
    net_amount          DECIMAL(10,2)  NOT NULL,
    vat_amount          DECIMAL(10,2)  NOT NULL,
    total_amount        DECIMAL(10,2)  NOT NULL,
    created_at          DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)             DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT FK_line_items_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE estimate_line_items (
    estimate_id  BIGINT NOT NULL,
    line_item_id BIGINT NOT NULL,
    PRIMARY KEY (estimate_id, line_item_id),
    CONSTRAINT FK_eli_estimate  FOREIGN KEY (estimate_id)  REFERENCES estimates(id)  ON DELETE CASCADE,
    CONSTRAINT FK_eli_line_item FOREIGN KEY (line_item_id) REFERENCES line_items(id) ON DELETE CASCADE
) ENGINE=InnoDB;
