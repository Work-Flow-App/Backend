-- Backfill estimate_line_items for estimates that have estimate_documents but lost
-- their line-item rows when V23 dropped the old ManyToMany join table.
--
-- For each estimate_document snapshot, if the parent estimate has zero
-- estimate_line_items rows, reconstruct them from the frozen snapshot data.
-- Status is set to WAITING_APPROVAL (the state items were in when the document
-- was originally generated).
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
SELECT DISTINCT
    ed.estimate_id,
    'WAITING_APPROVAL',
    jlis.source_line_item_id,
    jlis.product_code,
    jlis.product_description,
    jlis.additional_details,
    jlis.unit_price,
    jlis.quantity,
    jlis.vat_rate,
    jlis.net_amount,
    jlis.vat_amount,
    jlis.total_amount
FROM job_line_item_snapshots jlis
JOIN estimate_documents ed ON ed.id = jlis.estimate_document_id
WHERE jlis.type = 'ESTIMATE_DOCUMENT'
  AND NOT EXISTS (
      SELECT 1
      FROM estimate_line_items eli
      WHERE eli.estimate_id = ed.estimate_id
  );
