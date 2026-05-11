CREATE TABLE invoice_line_item_snapshots (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    invoice_id          BIGINT        NOT NULL,
    source_line_item_id BIGINT        NULL,
    product_code        VARCHAR(50)   NOT NULL,
    product_description VARCHAR(255)  NOT NULL,
    additional_details  VARCHAR(500)  NULL,
    unit_price          DECIMAL(10,2) NOT NULL,
    core_or_sub         VARCHAR(10)   NOT NULL,
    quantity            DECIMAL(10,4) NOT NULL,
    vat_rate            DECIMAL(5,2)  NOT NULL,
    net_amount          DECIMAL(10,2) NOT NULL,
    vat_amount          DECIMAL(10,2) NOT NULL,
    total_amount        DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_snap_invoice FOREIGN KEY (invoice_id)
        REFERENCES invoices(id) ON DELETE CASCADE,
    INDEX idx_snap_invoice (invoice_id),
    INDEX idx_snap_source  (source_line_item_id)
) ENGINE=InnoDB;

INSERT INTO invoice_line_item_snapshots
    (invoice_id, source_line_item_id, product_code, product_description,
     additional_details, unit_price, core_or_sub, quantity, vat_rate,
     net_amount, vat_amount, total_amount)
SELECT
    ili.invoice_id,
    ili.line_item_id,
    li.product_code,
    li.product_description,
    li.additional_details,
    li.unit_price,
    li.core_or_sub,
    li.quantity,
    li.vat_rate,
    li.net_amount,
    li.vat_amount,
    li.total_amount
FROM invoice_line_items ili
JOIN line_items li ON li.id = ili.line_item_id;

ALTER TABLE invoice_line_items DROP FOREIGN KEY fk_inv_li_line_item;
ALTER TABLE invoice_line_items DROP FOREIGN KEY fk_inv_li_invoice;
DROP TABLE invoice_line_items;