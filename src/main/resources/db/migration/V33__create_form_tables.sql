-- 1. Create form_templates table
CREATE TABLE form_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_form_templates_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE
);

-- 2. Create form_fields table
CREATE TABLE form_fields (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    label VARCHAR(150) NOT NULL,
    type VARCHAR(50) NOT NULL,
    role_target VARCHAR(50) NOT NULL,
    required BOOLEAN NOT NULL DEFAULT FALSE,
    options TEXT,
    order_index INT,
    CONSTRAINT fk_form_fields_template FOREIGN KEY (template_id) REFERENCES form_templates(id) ON DELETE CASCADE
);

-- 3. Create form_submissions table
CREATE TABLE form_submissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id BIGINT NOT NULL,
    template_id BIGINT NOT NULL,
    worker_id BIGINT,
    title VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    submitted_at DATETIME(6),
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_form_submissions_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    CONSTRAINT fk_form_submissions_template FOREIGN KEY (template_id) REFERENCES form_templates(id) ON DELETE CASCADE,
    CONSTRAINT fk_form_submissions_worker FOREIGN KEY (worker_id) REFERENCES workers(id) ON DELETE SET NULL
);

-- 4. Create form_field_values table
CREATE TABLE form_field_values (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    field_id BIGINT NOT NULL,
    string_value TEXT,
    boolean_value BOOLEAN,
    date_value DATETIME(6),
    json_value JSON,
    file_url VARCHAR(255),
    file_name VARCHAR(255),
    file_type VARCHAR(255),
    CONSTRAINT fk_form_values_submission FOREIGN KEY (submission_id) REFERENCES form_submissions(id) ON DELETE CASCADE,
    CONSTRAINT fk_form_values_field FOREIGN KEY (field_id) REFERENCES form_fields(id) ON DELETE CASCADE
);

-- Create indexes for performance on frequent lookup columns
CREATE INDEX idx_form_templates_company ON form_templates(company_id, archived);
CREATE INDEX idx_form_submissions_worker ON form_submissions(worker_id);
CREATE INDEX idx_form_submissions_company ON form_submissions(company_id);