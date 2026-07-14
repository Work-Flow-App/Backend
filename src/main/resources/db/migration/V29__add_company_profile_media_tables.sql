-- 1. Add new columns to the existing 'companies' table
ALTER TABLE companies
    ADD COLUMN logo_url VARCHAR(500) DEFAULT NULL,
    ADD COLUMN description TEXT DEFAULT NULL,
    ADD COLUMN website VARCHAR(255) DEFAULT NULL,
    ADD COLUMN tagline VARCHAR(255) DEFAULT NULL;

-- 2. Create the 'company_documents' table
CREATE TABLE company_documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT DEFAULT NULL,
    type VARCHAR(50) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(100) NOT NULL,
    validity_start_date DATE DEFAULT NULL,
    validity_end_date DATE DEFAULT NULL,
    is_public BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_company_documents_company_id FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE
);

-- 3. Create the 'company_posts' table
CREATE TABLE company_posts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    is_public BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_company_posts_company_id FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    CONSTRAINT fk_company_posts_author_id FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 4. Create the 'company_post_attachments' table
CREATE TABLE company_post_attachments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(100) NOT NULL,
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_company_post_attachments_post_id FOREIGN KEY (post_id) REFERENCES company_posts(id) ON DELETE CASCADE
);