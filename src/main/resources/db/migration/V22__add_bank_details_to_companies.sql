CREATE TABLE company_bank_details (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    company_id   BIGINT       NOT NULL UNIQUE,
    bank_name    VARCHAR(100),
    account_name VARCHAR(100),
    account_no   VARCHAR(50),
    sort_code    VARCHAR(20),
    created_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   DATETIME(6)           DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT FK_bank_details_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE
) ENGINE=InnoDB;
