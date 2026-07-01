-- V29__create_asset_attachments_table.sql

CREATE TABLE asset_attachments (
    asset_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(100) NOT NULL,
    file_url VARCHAR(1000) NOT NULL,
    
    -- Foreign key to link back to the main assets table
    CONSTRAINT fk_asset_attachments_asset_id 
        FOREIGN KEY (asset_id) 
        REFERENCES assets (id) 
        ON DELETE CASCADE
);

-- Optional but recommended: Add an index on asset_id since Hibernate will query this table by asset_id frequently
CREATE INDEX idx_asset_attachments_asset_id ON asset_attachments (asset_id);