ALTER TABLE invoices
    ADD COLUMN due_date  DATE         NULL,
    ADD COLUMN reference VARCHAR(100) NULL;
