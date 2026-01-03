-- Create worker invitations table for email-based worker signup
CREATE TABLE worker_invitations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    invitation_token VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(100) NOT NULL,
    company_id BIGINT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    used_at TIMESTAMP NULL,

    CONSTRAINT fk_worker_invitation_company
        FOREIGN KEY (company_id)
        REFERENCES companies(id)
        ON DELETE CASCADE,

    INDEX idx_invitation_token (invitation_token),
    INDEX idx_email (email),
    INDEX idx_company_expires (company_id, expires_at),
    INDEX idx_expires_used (expires_at, used)
) ENGINE=InnoDB;
