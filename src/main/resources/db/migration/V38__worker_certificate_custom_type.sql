-- Free-text label used when a certificate's type is OTHER, so admins/workers can name the
-- actual type instead of being stuck with a generic "Other" bucket.
ALTER TABLE worker_certificates
    ADD COLUMN custom_type_label VARCHAR(100) DEFAULT NULL;
