ALTER TABLE users
    ADD COLUMN google_id VARCHAR(255) NULL,
    ADD UNIQUE KEY UK_users_google_id (google_id);
