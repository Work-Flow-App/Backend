INSERT INTO users (uuid, username, password, role, enabled, created_at, updated_at)
VALUES (
    UUID(),  -- generates a new UUID
    'admin', 
    '$2a$10$.C2aZK5d63TyGCxrvr23TOJ1jhLZMkh2RV1yMyiHHpfw.ecZsaOKS', -- BCrypt password
    'ADMIN',
    TRUE,
    NOW(),
    NOW()
);
