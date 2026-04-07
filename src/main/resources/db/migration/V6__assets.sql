-- ============================================
--  ASSETS
-- ============================================
CREATE TABLE assets (
    id                BIGINT         AUTO_INCREMENT PRIMARY KEY,
    company_id        BIGINT         NOT NULL,
    name              VARCHAR(150)   NOT NULL,
    description       VARCHAR(500),
    serial_number     VARCHAR(100),
    asset_tag         VARCHAR(50),
    purchase_price    DECIMAL(19,2)  NOT NULL,
    purchase_date     DATE           NOT NULL,
    depreciation_rate DECIMAL(5,2)   NOT NULL,
    salvage_value     DECIMAL(19,2),
    asset_ref         BIGINT         NOT NULL DEFAULT 0,
    available         BOOLEAN        NOT NULL DEFAULT TRUE,
    archived          BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_assets_company        FOREIGN KEY (company_id) REFERENCES companies(id),
    CONSTRAINT uq_assets_company_name   UNIQUE (company_id, name),
    -- MySQL allows multiple NULL values in a unique index (NULL != NULL in SQL),
    -- so assets without an asset_tag (NULL) can coexist within the same company.
    CONSTRAINT uq_company_asset_tag     UNIQUE (company_id, asset_tag),
    CONSTRAINT uq_assets_company_serial UNIQUE (company_id, serial_number)
) ENGINE=InnoDB;

-- ============================================
--  ASSET JOB ASSIGNMENTS
-- ============================================
CREATE TABLE asset_job_assignments (
    id                BIGINT      AUTO_INCREMENT PRIMARY KEY,
    asset_id          BIGINT      NOT NULL,
    job_id            BIGINT,
    assigned_worker_id BIGINT,
    notes             VARCHAR(500),
    assigned_at       TIMESTAMP   NOT NULL,
    returned_at       TIMESTAMP,
    CONSTRAINT fk_assignment_asset  FOREIGN KEY (asset_id)           REFERENCES assets(id),
    CONSTRAINT fk_assignment_job    FOREIGN KEY (job_id)             REFERENCES jobs(id),
    CONSTRAINT fk_assignment_worker FOREIGN KEY (assigned_worker_id) REFERENCES workers(id),
    INDEX idx_assignment_asset   (asset_id),
    INDEX idx_assignment_job     (job_id),
    INDEX idx_assignment_worker  (assigned_worker_id)
) ENGINE=InnoDB;
