-- Drop the global unique constraint on verification_code
-- and replace with a composite unique on (user_id, verification_code)
-- so codes are only unique per user, preventing cross-user lookup and DB collisions
ALTER TABLE password_reset_tokens
    DROP INDEX verification_code,
    ADD CONSTRAINT uq_prt_user_code UNIQUE (user_id, verification_code);
