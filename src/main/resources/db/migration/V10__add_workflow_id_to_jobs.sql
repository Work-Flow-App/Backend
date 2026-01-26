-- Add workflow_id column to jobs table to store the workflow template associated with each job
ALTER TABLE jobs
    ADD COLUMN workflow_id BIGINT NULL AFTER assigned_worker_id;

-- Add foreign key constraint
ALTER TABLE jobs
    ADD CONSTRAINT fk_job_workflow FOREIGN KEY (workflow_id) REFERENCES workflows(id);

-- Add index for better query performance
CREATE INDEX idx_job_workflow ON jobs(workflow_id);