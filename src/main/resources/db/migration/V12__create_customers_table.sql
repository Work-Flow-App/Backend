-- ============================================
-- V12 - Create customers table
-- ============================================

CREATE TABLE customers (
    id          BIGINT NOT NULL AUTO_INCREMENT,
    name        VARCHAR(150) NOT NULL,
    company_id  BIGINT NOT NULL,
    email       VARCHAR(100),
    telephone   VARCHAR(20),
    mobile      VARCHAR(20),
    house_number VARCHAR(20),
    street      VARCHAR(150),
    city        VARCHAR(100),
    county      VARCHAR(100),
    postal_code VARCHAR(20),
    country     VARCHAR(100),
    archived    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6) DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT FK_customers_company       FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    CONSTRAINT uq_customers_company_name  UNIQUE (company_id, name),
    CONSTRAINT uq_customers_company_email UNIQUE (company_id, email)
) ENGINE=InnoDB;
