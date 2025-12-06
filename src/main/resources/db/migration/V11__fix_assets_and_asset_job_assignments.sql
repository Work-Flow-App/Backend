-- ================================
-- DROP OLD TABLES
-- ================================
DROP TABLE IF EXISTS asset_job_assignments;
DROP TABLE IF EXISTS assets;

-- ================================
-- CREATE assets TABLE
-- ================================

CREATE TABLE assets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    serial_number VARCHAR(100),
    asset_tag VARCHAR(50),
    purchase_price DECIMAL(19,2) NOT NULL,
    purchase_date DATE NOT NULL,
    depreciation_rate DECIMAL(5,2) NOT NULL,
    salvage_value DECIMAL(19,2),
    current_location VARCHAR(255),
    latitude DOUBLE,
    longitude DOUBLE,
    available BOOLEAN NOT NULL DEFAULT TRUE,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);


-- Unique constraints
ALTER TABLE assets
    ADD CONSTRAINT uq_company_name UNIQUE (company_id, name);

ALTER TABLE assets
    ADD CONSTRAINT uq_company_asset_tag UNIQUE (company_id, asset_tag);

-- Foreign key constraint to company table
ALTER TABLE assets
    ADD CONSTRAINT fk_assets_company
    FOREIGN KEY (company_id) REFERENCES companies(id);

-- ================================
-- CREATE asset_job_assignments TABLE
-- ================================

CREATE TABLE asset_job_assignments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    asset_id BIGINT NOT NULL,
    job_id BIGINT,
    assigned_worker_id BIGINT,
    notes VARCHAR(500),
    assigned_at TIMESTAMP NOT NULL,
    returned_at TIMESTAMP
);


-- Indexes
CREATE INDEX idx_assignment_asset ON asset_job_assignments(asset_id);
CREATE INDEX idx_assignment_job ON asset_job_assignments(job_id);

-- Foreign key constraints
ALTER TABLE asset_job_assignments
    ADD CONSTRAINT fk_assignment_asset
    FOREIGN KEY (asset_id) REFERENCES assets(id);

ALTER TABLE asset_job_assignments
    ADD CONSTRAINT fk_assignment_job
    FOREIGN KEY (job_id) REFERENCES jobs(id);

ALTER TABLE asset_job_assignments
    ADD CONSTRAINT fk_assignment_worker
    FOREIGN KEY (assigned_worker_id) REFERENCES workers(id);
