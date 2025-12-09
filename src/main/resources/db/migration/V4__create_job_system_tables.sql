-- ============================================
--   JOB SYSTEM TABLES
-- ============================================

-- ----------------------------
-- Table: job_templates
-- ----------------------------
CREATE TABLE job_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_job_template_company
        FOREIGN KEY (company_id)
        REFERENCES companies(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_company_name UNIQUE (company_id, name),
    INDEX idx_company (company_id)
) ENGINE=InnoDB;


-- ----------------------------
-- Table: job_template_fields
-- ----------------------------
CREATE TABLE job_template_fields (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    label VARCHAR(150) NOT NULL,
    job_field_type ENUM('TEXT','NUMBER','DATE','BOOLEAN','DROPDOWN','JSON','REFERENCE') NOT NULL,
    required BOOLEAN NOT NULL DEFAULT FALSE,
    options TEXT NULL,
    order_index INT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_field_template
        FOREIGN KEY (template_id)
        REFERENCES job_templates(id)
        ON DELETE CASCADE,

    INDEX idx_template (template_id),
    INDEX idx_template_order (template_id, order_index)
) ENGINE=InnoDB;


-- ----------------------------
-- Table: jobs
-- ----------------------------
CREATE TABLE jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    client_id BIGINT NULL,
    assigned_worker_id BIGINT NULL,
    status ENUM('NEW', 'PENDING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'NEW',
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_job_template
        FOREIGN KEY (template_id)
        REFERENCES job_templates(id),

    CONSTRAINT fk_job_company
        FOREIGN KEY (company_id)
        REFERENCES companies(id),

    CONSTRAINT fk_job_client
        FOREIGN KEY (client_id)
        REFERENCES clients(id),

    CONSTRAINT fk_job_worker
        FOREIGN KEY (assigned_worker_id)
        REFERENCES workers(id),

    INDEX idx_template (template_id),
    INDEX idx_company (company_id),
    INDEX idx_client (client_id),
    INDEX idx_worker (assigned_worker_id),
    INDEX idx_status (status)
) ENGINE=InnoDB;


-- ----------------------------
-- Table: job_field_values
-- ----------------------------
CREATE TABLE job_field_values (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id BIGINT NOT NULL,
    field_id BIGINT NOT NULL,
    value TEXT NULL,
    string_value TEXT NULL,
    number_value DOUBLE NULL,
    boolean_value BOOLEAN NULL,
    date_value TIMESTAMP NULL,
    json_value JSON NULL,
    reference_id BIGINT NULL,
    reference_type VARCHAR(150) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_value_job
        FOREIGN KEY (job_id)
        REFERENCES jobs(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_value_field
        FOREIGN KEY (field_id)
        REFERENCES job_template_fields(id)
        ON DELETE CASCADE,

    UNIQUE KEY unique_job_field (job_id, field_id),
    INDEX idx_job (job_id),
    INDEX idx_field (field_id)
) ENGINE=InnoDB;