-- Allow 'actor_id' to be NULL for system-generated activities (e.g., SLA breaches)
ALTER TABLE job_workflow_step_activities 
MODIFY actor_id BIGINT NULL;