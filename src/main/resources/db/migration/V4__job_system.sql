-- ============================================
--  ADDRESSES
-- ============================================
CREATE TABLE addresses (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    street          VARCHAR(255),
    city            VARCHAR(255),
    state           VARCHAR(255),
    postal_code     VARCHAR(255),
    country         VARCHAR(255),
    additional_info TEXT,
    latitude        DOUBLE,
    longitude       DOUBLE
) ENGINE=InnoDB;

-- ============================================
--  CUSTOMERS
-- ============================================
CREATE TABLE customers (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    name         VARCHAR(150) NOT NULL,
    company_id   BIGINT       NOT NULL,
    email        VARCHAR(100),
    telephone    VARCHAR(20),
    mobile       VARCHAR(20),
    house_number VARCHAR(20),
    street       VARCHAR(150),
    city         VARCHAR(100),
    county       VARCHAR(100),
    postal_code  VARCHAR(20),
    country      VARCHAR(100),
    customer_ref BIGINT       NOT NULL DEFAULT 0,
    archived     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   DATETIME(6)           DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT FK_customers_company       FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    CONSTRAINT uq_customers_company_name  UNIQUE (company_id, name),
    CONSTRAINT uq_customers_company_email UNIQUE (company_id, email)
) ENGINE=InnoDB;

-- ============================================
--  JOB TEMPLATES
-- ============================================
CREATE TABLE job_templates (
    id           BIGINT       AUTO_INCREMENT PRIMARY KEY,
    company_id   BIGINT       NOT NULL,
    name         VARCHAR(150) NOT NULL,
    description  VARCHAR(255),
    is_default   BOOLEAN      NOT NULL DEFAULT FALSE,
    template_ref BIGINT       NOT NULL DEFAULT 0,
    created_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_job_template_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    CONSTRAINT uq_company_name UNIQUE (company_id, name),
    INDEX idx_company    (company_id),
    INDEX idx_is_default (is_default)
) ENGINE=InnoDB;

-- ============================================
--  JOB TEMPLATE FIELDS
-- ============================================
CREATE TABLE job_template_fields (
    id             BIGINT      AUTO_INCREMENT PRIMARY KEY,
    template_id    BIGINT      NOT NULL,
    name           VARCHAR(150) NOT NULL,
    label          VARCHAR(150) NOT NULL,
    job_field_type ENUM('TEXT','NUMBER','DATE','BOOLEAN','DROPDOWN','JSON','REFERENCE') NOT NULL,
    required       BOOLEAN     NOT NULL DEFAULT FALSE,
    options        TEXT        NULL,
    order_index    INT         NULL,
    created_at     TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_field_template FOREIGN KEY (template_id) REFERENCES job_templates(id) ON DELETE CASCADE,
    INDEX idx_template       (template_id),
    INDEX idx_template_order (template_id, order_index)
) ENGINE=InnoDB;

-- ============================================
--  JOBS
-- ============================================
CREATE TABLE jobs (
    id                 BIGINT    AUTO_INCREMENT PRIMARY KEY,
    template_id        BIGINT    NOT NULL,
    company_id         BIGINT    NOT NULL,
    customer_id        BIGINT    NOT NULL,
    client_id          BIGINT    NULL,
    assigned_worker_id BIGINT    NULL,
    workflow_id        BIGINT    NULL,
    address_id         BIGINT    NULL,
    job_ref            BIGINT    NOT NULL DEFAULT 0,
    status             ENUM('NEW','PENDING','IN_PROGRESS','COMPLETED','CANCELLED') NOT NULL DEFAULT 'NEW',
    archived           BOOLEAN   NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_job_template  FOREIGN KEY (template_id)        REFERENCES job_templates(id),
    CONSTRAINT fk_job_company   FOREIGN KEY (company_id)         REFERENCES companies(id),
    CONSTRAINT fk_job_customer  FOREIGN KEY (customer_id)        REFERENCES customers(id),
    CONSTRAINT fk_job_client    FOREIGN KEY (client_id)          REFERENCES clients(id),
    CONSTRAINT fk_job_worker    FOREIGN KEY (assigned_worker_id) REFERENCES workers(id),
    CONSTRAINT fk_job_workflow  FOREIGN KEY (workflow_id)        REFERENCES workflows(id),
    CONSTRAINT fk_jobs_address  FOREIGN KEY (address_id)         REFERENCES addresses(id) ON DELETE SET NULL,
    INDEX idx_template  (template_id),
    INDEX idx_company   (company_id),
    INDEX idx_customer  (customer_id),
    INDEX idx_client    (client_id),
    INDEX idx_worker    (assigned_worker_id),
    INDEX idx_status    (status),
    INDEX idx_workflow  (workflow_id),
    INDEX idx_address   (address_id)
) ENGINE=InnoDB;

-- ============================================
--  JOB FIELD VALUES
-- ============================================
CREATE TABLE job_field_values (
    id             BIGINT    AUTO_INCREMENT PRIMARY KEY,
    job_id         BIGINT    NOT NULL,
    field_id       BIGINT    NOT NULL,
    string_value   TEXT      NULL,
    number_value   DOUBLE    NULL,
    boolean_value  BOOLEAN   NULL,
    date_value     TIMESTAMP NULL,
    json_value     JSON      NULL,
    reference_id   BIGINT    NULL,
    reference_type VARCHAR(150) NULL,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_value_job   FOREIGN KEY (job_id)   REFERENCES jobs(id)                ON DELETE CASCADE,
    CONSTRAINT fk_value_field FOREIGN KEY (field_id) REFERENCES job_template_fields(id) ON DELETE CASCADE,
    UNIQUE KEY unique_job_field (job_id, field_id),
    INDEX idx_job   (job_id),
    INDEX idx_field (field_id)
) ENGINE=InnoDB;
