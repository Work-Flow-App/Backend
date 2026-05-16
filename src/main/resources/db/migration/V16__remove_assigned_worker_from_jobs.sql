-- Drop the foreign key constraint
ALTER TABLE jobs 
DROP FOREIGN KEY fk_job_worker;

-- Drop the index associated with the column
ALTER TABLE jobs 
DROP INDEX idx_worker;

-- Drop the column
ALTER TABLE jobs 
DROP COLUMN assigned_worker_id;