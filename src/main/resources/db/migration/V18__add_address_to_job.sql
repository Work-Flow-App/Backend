-- V18__add_address_to_job.sql

-- 1. Create addresses table (MATCHES ENTITY EXACTLY)
CREATE TABLE addresses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    street VARCHAR(255),
    city VARCHAR(255),
    state VARCHAR(255),
    postal_code VARCHAR(255),
    country VARCHAR(255),

    additional_info TEXT,

    latitude DOUBLE,
    longitude DOUBLE
);

-- 2. Add address_id column to jobs table
ALTER TABLE jobs
ADD COLUMN address_id BIGINT NULL;

-- 3. Add foreign key constraint
ALTER TABLE jobs
ADD CONSTRAINT fk_jobs_address
FOREIGN KEY (address_id) REFERENCES addresses(id)
ON DELETE SET NULL;

-- 4. Add index (recommended)
CREATE INDEX idx_jobs_address_id ON jobs(address_id);