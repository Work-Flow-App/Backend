CREATE TABLE company_post_groups (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_cpg_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    UNIQUE KEY uk_company_group_name (company_id, name)
);

-- Add group reference to posts
ALTER TABLE company_posts
ADD COLUMN group_id BIGINT NULL;

-- Restrict deletion of a group if posts are still attached
ALTER TABLE company_posts
ADD CONSTRAINT fk_cp_group FOREIGN KEY (group_id) REFERENCES company_post_groups(id) ON DELETE RESTRICT;