-- ============================================
--  USERS
-- ============================================
CREATE TABLE users (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    uuid       VARCHAR(255) NOT NULL,
    username   VARCHAR(50)  NOT NULL,
    password   VARCHAR(255) NOT NULL,
    email      VARCHAR(255),
    google_id  VARCHAR(255) NULL,
    role       ENUM('ADMIN','COMPANY','WORKER') NOT NULL,
    enabled    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6)           DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY UK_users_username (username),
    UNIQUE KEY UK_users_email    (email),
    UNIQUE KEY UK_users_uuid     (uuid),
    UNIQUE KEY UK_users_google_id (google_id)
) ENGINE=InnoDB AUTO_INCREMENT=1001;

-- ============================================
--  REFRESH TOKENS
-- ============================================
CREATE TABLE refresh_tokens (
    id           BIGINT       AUTO_INCREMENT PRIMARY KEY,
    token        VARCHAR(255) NOT NULL UNIQUE,
    user_id      BIGINT       NOT NULL,
    expires_at   TIMESTAMP    NOT NULL,
    revoked      BOOLEAN      NOT NULL DEFAULT FALSE,
    device_info  VARCHAR(500),
    ip_address   VARCHAR(45),
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP    NULL,
    revoked_at   TIMESTAMP    NULL,
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_token          (token),
    INDEX idx_user_id        (user_id),
    INDEX idx_expires_at     (expires_at),
    INDEX idx_revoked        (revoked),
    INDEX idx_user_revoked   (user_id, revoked)
) ENGINE=InnoDB;

-- ============================================
--  PASSWORD RESET TOKENS
-- ============================================
CREATE TABLE password_reset_tokens (
    id                BIGINT      PRIMARY KEY AUTO_INCREMENT,
    verification_code VARCHAR(6)  NOT NULL UNIQUE,
    user_id           BIGINT      NOT NULL,
    expires_at        TIMESTAMP   NOT NULL,
    used              BOOLEAN     DEFAULT FALSE,
    created_at        TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    used_at           TIMESTAMP   NULL,
    CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_verification_code (verification_code),
    INDEX idx_user_expires      (user_id, expires_at),
    INDEX idx_expires_used      (expires_at, used)
) ENGINE=InnoDB;

-- ============================================
--  EMAIL VERIFICATION TOKENS
-- ============================================
CREATE TABLE email_verification_tokens (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    token      VARCHAR(255) NOT NULL UNIQUE,
    user_id    BIGINT       NOT NULL,
    expires_at DATETIME     NOT NULL,
    used       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at DATETIME,
    used_at    DATETIME,
    PRIMARY KEY (id),
    CONSTRAINT FK_email_verification_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_email_verification_token (token),
    INDEX idx_email_verification_user  (user_id)
) ENGINE=InnoDB;
