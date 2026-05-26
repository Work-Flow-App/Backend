-- Drop columns removed from the library line_items table
ALTER TABLE line_items
    DROP COLUMN core_or_sub,
    DROP COLUMN invoiced;
