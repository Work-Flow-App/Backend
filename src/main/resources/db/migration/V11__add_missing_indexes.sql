-- ============================================
--  MISSING INDEXES (Performance Optimization)
-- ============================================

-- A-3: clients — composite index for common filtered queries (company + archived filter)
ALTER TABLE clients
    ADD INDEX idx_clients_company_archived (company_id, archived);

-- A-6: workflow_steps — composite index covering FK lookup + ordering in one index
ALTER TABLE workflow_steps
    ADD INDEX idx_workflow_steps_workflow_order (workflow_id, order_index);

-- A-8: job_workflow_steps — composite index for the high-frequency findByJobWorkflowIdOrderByOrderIndexAsc query
ALTER TABLE job_workflow_steps
    ADD INDEX idx_jws_workflow_order (job_workflow_id, order_index);

-- A-9: job_workflow_step_workers — reverse index on worker_id for worker-based lookups
ALTER TABLE job_workflow_step_workers
    ADD INDEX idx_jwsw_worker_id (worker_id);

-- A-12: workers — composite index for common company + archived filter pattern
ALTER TABLE workers
    ADD INDEX idx_workers_company_archived (company_id, archived);
