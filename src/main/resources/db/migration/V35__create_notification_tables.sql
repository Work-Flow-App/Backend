-- ---------------------------------------------------------
-- Table: notification_preferences
-- ---------------------------------------------------------
CREATE TABLE notification_preferences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    push_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Assuming your user table is named 'users'. Adjust if it is 'user' or something else.
    CONSTRAINT fk_notification_pref_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    -- Enforces the JPA @UniqueConstraint and serves as an index for findByUserIdAndNotificationType
    CONSTRAINT uk_user_notification_type UNIQUE (user_id, notification_type)
);

-- ---------------------------------------------------------
-- Table: device_tokens
-- ---------------------------------------------------------
CREATE TABLE device_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    platform VARCHAR(50) NOT NULL,
    push_token VARCHAR(512) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    
    CONSTRAINT fk_device_token_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    -- Unique constraint explicitly requested on push_token
    CONSTRAINT uk_push_token UNIQUE (push_token)
);

-- INDEX: Speeds up findByUserIdAndIsActiveTrue(Long userId) when dispatching push notifications
CREATE INDEX idx_device_token_user_active ON device_tokens(user_id, is_active);

-- ---------------------------------------------------------
-- Table: notifications
-- ---------------------------------------------------------
CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,         
    target_url VARCHAR(512),       -- Sized to 512 to accommodate longer query parameters if needed
    entity_type VARCHAR(100),
    entity_id BIGINT,
    priority VARCHAR(50) NOT NULL,
    metadata JSON,                 -- Native JSON mapping
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at DATETIME(6),
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_notif_user_read_id ON notifications(user_id, is_read, id DESC);

-- INDEX 2: Polymorphic lookup index. Useful if you ever need to bulk-delete or update 
-- notifications when the underlying entity (like a Comment or Job) is deleted.
CREATE INDEX idx_notif_entity ON notifications(entity_type, entity_id);