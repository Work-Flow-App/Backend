-- Backfill estimate_line_items for estimates that have estimate_documents but lost
-- their line-item rows when V23 dropped the old ManyToMany join table.
--
-- Deduplicates by (estimate_id, source_line_item_id) to match the original
-- ManyToMany cardinality (one library item per estimate). Uses MAX() for the
-- denormalised amount columns — all snapshots for the same source item on the
-- same estimate are identical in practice.
--
-- Status logic:
--   INVOICED  — if the source item also appears in an INVOICE-type snapshot for
--               this estimate (i.e. it was billed). Must not be made editable.
--   WAITING_APPROVAL — all other cases (document generated but not yet invoiced).
--
-- NOTE: after this backfill, snapshot.source_line_item_id still points to the
-- old line_items.id (library IDs). cleanupEmptyDocuments guards against this
-- by checking for orphaned references before deleting documents.

INSERT INTO estimate_line_items (
    estimate_id,
    status,
    source_line_item_id,
    product_code,
    product_description,
    additional_details,
    unit_price,
    quantity,
    vat_rate,
    net_amount,
    vat_amount,
    total_amount
)
SELECT
    ed.estimate_id,
    CASE
        WHEN EXISTS (
            SELECT 1
            FROM job_line_item_snapshots inv_snap
            JOIN invoices i ON i.id = inv_snap.invoice_id
            WHERE inv_snap.type        = 'INVOICE'
              AND inv_snap.source_line_item_id = jlis.source_line_item_id
              AND i.estimate_id        = ed.estimate_id
        ) THEN 'INVOICED'
        ELSE 'WAITING_APPROVAL'
    END,
    jlis.source_line_item_id,
    MAX(jlis.product_code),
    MAX(jlis.product_description),
    MAX(jlis.additional_details),
    MAX(jlis.unit_price),
    MAX(jlis.quantity),
    MAX(jlis.vat_rate),
    MAX(jlis.net_amount),
    MAX(jlis.vat_amount),
    MAX(jlis.total_amount)
FROM job_line_item_snapshots jlis
JOIN estimate_documents ed ON ed.id = jlis.estimate_document_id
WHERE jlis.type = 'ESTIMATE_DOCUMENT'
  AND NOT EXISTS (
      SELECT 1
      FROM estimate_line_items eli
      WHERE eli.estimate_id = ed.estimate_id
  )
GROUP BY ed.estimate_id, jlis.source_line_item_id;
