INSERT INTO users (uuid, username, password, role, enabled, created_at, updated_at)
VALUES (
    UUID(),  -- generates a new UUID
    'admin', 
    '$2a$10$7Qm5EwuvvMb1ZnXJ0fLqI.1F1D0GvUxu6mDdIogU3vY7ZKKr0ErPu', -- BCrypt password
    'ADMIN',
    TRUE,
    NOW(),
    NOW()
);
