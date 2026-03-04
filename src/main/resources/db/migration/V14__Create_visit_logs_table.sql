-- ============================================
-- V14 - Create job_workflow_step_visit_logs table
-- ============================================

CREATE TABLE job_workflow_step_visit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_workflow_step_id BIGINT NOT NULL,
    logged_by_id BIGINT NOT NULL,
    visit_date DATE NOT NULL,
    time_in TIME NOT NULL,
    time_out TIME DEFAULT NULL,
    description TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
    
    -- Foreign Keys
    CONSTRAINT fk_visit_log_step 
        FOREIGN KEY (job_workflow_step_id) 
        REFERENCES job_workflow_steps(id),
        
    CONSTRAINT fk_visit_log_user 
        FOREIGN KEY (logged_by_id) 
        REFERENCES users(id) -- Adjust 'users' to your actual user table name if different
);

-- Indexes for performance (frequently queried by stepId)
CREATE INDEX idx_visit_log_step ON job_workflow_step_visit_logs(job_workflow_step_id);
CREATE INDEX idx_visit_log_date_time ON job_workflow_step_visit_logs(visit_date, time_in);