-- ============================================
--   CREATE ASSET & ASSET JOB ASSIGNMENT TABLES
-- ============================================

-- ===========================
-- 1. ASSETS TABLE
-- ===========================
CREATE TABLE assets (
    id BIGINT NOT NULL AUTO_INCREMENT,
    asset_code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    category VARCHAR(255) NULL,

    purchase_price DECIMAL(15,2) NOT NULL,
    purchase_date DATE NOT NULL,
    depreciation_rate DECIMAL(5,2) NOT NULL,
    salvage_value DECIMAL(15,2) NOT NULL,

    barcode VARCHAR(255) NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'AVAILABLE',

    current_location VARCHAR(255) NULL,
    latitude DOUBLE NULL,
    longitude DOUBLE NULL,

    archived BOOLEAN NOT NULL DEFAULT FALSE,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    -- Prevent duplicate asset codes
    CONSTRAINT uq_asset_code UNIQUE (asset_code),
    -- Optional but common uniqueness
    UNIQUE KEY uq_asset_barcode (barcode)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



-- ===========================
-- 2. ASSET JOB ASSIGNMENT TABLE
-- ===========================
CREATE TABLE asset_job_assignments (
    id BIGINT NOT NULL AUTO_INCREMENT,

    asset_id BIGINT NOT NULL,
    job_id BIGINT NOT NULL,

    assigned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by VARCHAR(255) NULL,

    returned_at DATETIME NULL,
    returned_by VARCHAR(255) NULL,

    notes TEXT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    CONSTRAINT fk_assignment_asset
        FOREIGN KEY (asset_id)
        REFERENCES assets(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_assignment_job
        FOREIGN KEY (job_id)
        REFERENCES jobs(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ===========================
-- INDEXES FOR PERFORMANCE
-- ===========================
CREATE INDEX idx_asset_status ON assets(status);
CREATE INDEX idx_asset_category ON assets(category);
CREATE INDEX idx_assignment_asset ON asset_job_assignments(asset_id);
CREATE INDEX idx_assignment_job ON asset_job_assignments(job_id);
CREATE INDEX idx_assignment_assigned_at ON asset_job_assignments(assigned_at);
