CREATE TABLE email_verification_tokens (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    token      VARCHAR(255) NOT NULL UNIQUE,
    user_id    BIGINT       NOT NULL,
    expires_at DATETIME     NOT NULL,
    used       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at DATETIME,
    used_at    DATETIME,
    PRIMARY KEY (id),
    CONSTRAINT FK_email_verification_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_email_verification_token (token),
    INDEX idx_email_verification_user (user_id)
);

ALTER TABLE users
    MODIFY COLUMN enabled BOOLEAN NOT NULL DEFAULT FALSE;
