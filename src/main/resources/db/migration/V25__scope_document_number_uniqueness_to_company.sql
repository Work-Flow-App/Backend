-- Document numbers (estimates and invoices) are generated from per-company
-- counters, so uniqueness must be scoped to company_id, not global.
-- A global unique index collides when two companies both generate their
-- first document (both get EST-2026-00001 / INV-2026-00001).

ALTER TABLE estimate_documents
    DROP INDEX uq_est_doc_number,
    ADD UNIQUE INDEX uq_est_doc_company_number (company_id, document_number);

ALTER TABLE invoices
    DROP INDEX invoice_number,
    ADD UNIQUE INDEX uq_inv_company_number (company_id, invoice_number);
