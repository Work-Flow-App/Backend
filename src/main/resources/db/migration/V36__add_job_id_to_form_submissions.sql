-- Add the new column
ALTER TABLE form_submissions 
ADD COLUMN job_id BIGINT NULL;

-- Add the foreign key constraint
ALTER TABLE form_submissions 
ADD CONSTRAINT fk_form_submissions_job_id 
FOREIGN KEY (job_id) REFERENCES jobs(id);