CREATE TABLE company_member_invitations (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    invitation_token VARCHAR(255) NOT NULL UNIQUE,
    email            VARCHAR(100) NOT NULL,
    company_id       BIGINT       NOT NULL,
    company_role     ENUM('MANAGER','EDITOR','VIEWER') NOT NULL,
    expires_at       TIMESTAMP    NOT NULL,
    used             BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    used_at          TIMESTAMP    NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_cmi_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    INDEX idx_cmi_token   (invitation_token),
    INDEX idx_cmi_email   (email),
    INDEX idx_cmi_company (company_id, expires_at)
) ENGINE=InnoDB;
