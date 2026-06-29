-- Migration V28: Add location tracking to assets and asset_job_assignments

-- 1. Update `assets` table
ALTER TABLE assets 
    ADD COLUMN location_type VARCHAR(30) NOT NULL DEFAULT 'WAREHOUSE',
    ADD COLUMN address_id BIGINT DEFAULT NULL,
    ADD COLUMN warehouse_address_id BIGINT DEFAULT NULL;

-- Add foreign key constraints for the new address relationships on `assets`
ALTER TABLE assets 
    ADD CONSTRAINT fk_asset_current_address 
    FOREIGN KEY (address_id) REFERENCES addresses(id) ON DELETE SET NULL,
    
    ADD CONSTRAINT fk_asset_warehouse_address 
    FOREIGN KEY (warehouse_address_id) REFERENCES addresses(id) ON DELETE SET NULL;


-- 2. Update `asset_job_assignments` table
ALTER TABLE asset_job_assignments 
    ADD COLUMN location_type VARCHAR(30) DEFAULT NULL,
    ADD COLUMN address_id BIGINT DEFAULT NULL;

-- Add foreign key constraint for the address relationship on `asset_job_assignments`
ALTER TABLE asset_job_assignments 
    ADD CONSTRAINT fk_assignment_address 
    FOREIGN KEY (address_id) REFERENCES addresses(id) ON DELETE SET NULL;