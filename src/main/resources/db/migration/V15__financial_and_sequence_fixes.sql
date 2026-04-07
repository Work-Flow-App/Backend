-- ============================================
--  V7-A-2: Standardize estimate_line_items FK to RESTRICT
--  Both join tables should use RESTRICT so a line item
--  cannot be silently deleted while referenced.
-- ============================================
ALTER TABLE estimate_line_items
    DROP FOREIGN KEY FK_eli_line_item,
    ADD CONSTRAINT fk_eli_line_item
        FOREIGN KEY (line_item_id) REFERENCES line_items(id) ON DELETE RESTRICT;

-- ============================================
--  V7-A-3: Protect invoices from cascade-delete via estimates
--  Deleting an estimate that has invoices must be blocked at DB level.
-- ============================================
ALTER TABLE invoices
    DROP FOREIGN KEY fk_invoices_estimate,
    ADD CONSTRAINT fk_invoices_estimate
        FOREIGN KEY (estimate_id) REFERENCES estimates(id) ON DELETE RESTRICT;

-- ============================================
--  V7-D-3: Add invoice sequence counter column
-- ============================================
ALTER TABLE company_counters ADD COLUMN next_invoice_id BIGINT NOT NULL DEFAULT 1;
