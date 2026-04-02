-- ============================================
--  COMPANIES
-- ============================================
CREATE TABLE companies (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    name           VARCHAR(150) NOT NULL,
    user_id        BIGINT       NOT NULL,
    address_line_1 VARCHAR(255),
    address_line_2 VARCHAR(255),
    address_line_3 VARCHAR(255),
    town           VARCHAR(100),
    country        VARCHAR(100),
    postcode       VARCHAR(20),
    telephone      VARCHAR(20),
    mobile         VARCHAR(20),
    fax            VARCHAR(20),
    email          VARCHAR(100),
    contact_email  VARCHAR(100),
    contact_number VARCHAR(50),
    vat_number     VARCHAR(50)  NULL,
    archived       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at     DATETIME(6)           DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY UK_companies_name (name),
    CONSTRAINT FK_companies_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;

-- ============================================
--  COMPANY BANK DETAILS
-- ============================================
CREATE TABLE company_bank_details (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    company_id   BIGINT      NOT NULL UNIQUE,
    bank_name    VARCHAR(100),
    account_name VARCHAR(100),
    account_no   VARCHAR(50),
    sort_code    VARCHAR(20),
    created_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   DATETIME(6)          DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT FK_bank_details_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================
--  CLIENTS
-- ============================================
CREATE TABLE clients (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    name       VARCHAR(150) NOT NULL,
    company_id BIGINT       NOT NULL,
    email      VARCHAR(100),
    telephone  VARCHAR(20),
    mobile     VARCHAR(20),
    address    VARCHAR(255),
    client_ref BIGINT       NOT NULL DEFAULT 0,
    archived   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6)           DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT FK_clients_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================
--  WORKERS
-- ============================================
CREATE TABLE workers (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    name         VARCHAR(100) NOT NULL,
    initials     VARCHAR(10),
    company_id   BIGINT       NOT NULL,
    user_id      BIGINT       NOT NULL UNIQUE,
    telephone    VARCHAR(20),
    mobile       VARCHAR(20),
    email        VARCHAR(100),
    worker_ref   BIGINT       NOT NULL DEFAULT 0,
    login_locked BOOLEAN      NOT NULL DEFAULT FALSE,
    archived     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   DATETIME(6)           DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT FK_workers_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    CONSTRAINT FK_workers_user    FOREIGN KEY (user_id)    REFERENCES users(id)     ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================
--  COMPANY MEMBERS
-- ============================================
CREATE TABLE company_members (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    company_id   BIGINT      NOT NULL,
    user_id      BIGINT      NOT NULL,
    company_role ENUM('COMPANY_ADMIN','MANAGER','EDITOR') NOT NULL,
    active       BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   DATETIME(6)          DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY UK_company_member (company_id, user_id),
    CONSTRAINT FK_company_members_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    CONSTRAINT FK_company_members_user    FOREIGN KEY (user_id)    REFERENCES users(id)     ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================
--  WORKER INVITATIONS
-- ============================================
CREATE TABLE worker_invitations (
    id               BIGINT       PRIMARY KEY AUTO_INCREMENT,
    invitation_token VARCHAR(255) NOT NULL UNIQUE,
    email            VARCHAR(100) NOT NULL,
    company_id       BIGINT       NOT NULL,
    expires_at       TIMESTAMP    NOT NULL,
    used             BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    used_at          TIMESTAMP    NULL,
    CONSTRAINT fk_worker_invitation_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    INDEX idx_invitation_token  (invitation_token),
    INDEX idx_email             (email),
    INDEX idx_company_expires   (company_id, expires_at),
    INDEX idx_expires_used      (expires_at, used)
) ENGINE=InnoDB;
