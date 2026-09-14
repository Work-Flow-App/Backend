-- 1. Create the new asset_groups table
CREATE TABLE asset_groups (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    created_at DATETIME(6),
    updated_at DATETIME(6),
    CONSTRAINT fk_asset_groups_company_id FOREIGN KEY (company_id) REFERENCES companies(id)
);

-- 2. Add the group_id column to the existing assets table
ALTER TABLE assets
ADD COLUMN group_id BIGINT;

-- 3. Add the foreign key constraint to link assets to asset_groups
ALTER TABLE assets
ADD CONSTRAINT fk_assets_group_id FOREIGN KEY (group_id) REFERENCES asset_groups(id);