-- V20__add_sla_fields_to_workflow_steps.sql

-- 1. Add SLA tracking to the blueprint templates
ALTER TABLE workflow_steps
ADD COLUMN expected_duration_minutes INT DEFAULT NULL,
ADD COLUMN maximum_duration_minutes INT DEFAULT NULL;

-- 2. Add SLA tracking and the circuit-breaker flag to the active job execution steps
ALTER TABLE job_workflow_steps
ADD COLUMN expected_duration_minutes INT DEFAULT NULL,
ADD COLUMN maximum_duration_minutes INT DEFAULT NULL,
ADD COLUMN sla_breached BOOLEAN DEFAULT FALSE NOT NULL;