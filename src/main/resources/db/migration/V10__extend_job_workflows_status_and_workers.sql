-- =========================================================
-- V10: extend job workflows status and workers
-- =========================================================

ALTER TABLE job_workflows
ADD COLUMN status VARCHAR(50) NOT NULL DEFAULT 'INITIATED';

CREATE TABLE job_workflow_workers (
    job_workflow_id BIGINT NOT NULL,
    worker_id BIGINT NOT NULL,

    PRIMARY KEY (job_workflow_id, worker_id),

    CONSTRAINT fk_jww_job_workflow
        FOREIGN KEY (job_workflow_id)
        REFERENCES job_workflows(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_jww_worker
        FOREIGN KEY (worker_id)
        REFERENCES workers(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
