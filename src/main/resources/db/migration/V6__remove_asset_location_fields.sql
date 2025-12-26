-- ============================================
--   REMOVE LOCATION FIELDS FROM ASSETS TABLE
-- ============================================

-- Remove location-related columns from assets table
ALTER TABLE assets
    DROP COLUMN current_location,
    DROP COLUMN latitude,
    DROP COLUMN longitude;
