-- ============================================
--  WORKFLOWS
-- ============================================
CREATE TABLE workflows (
    id           BIGINT      AUTO_INCREMENT PRIMARY KEY,
    company_id   BIGINT      NOT NULL,
    name         VARCHAR(150) NOT NULL,
    description  TEXT,
    workflow_ref BIGINT       NOT NULL DEFAULT 0,
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_workflows_company_name UNIQUE (company_id, name),
    CONSTRAINT fk_workflows_company FOREIGN KEY (company_id) REFERENCES companies(id)
) ENGINE=InnoDB;

-- ============================================
--  WORKFLOW STEPS
-- ============================================
CREATE TABLE workflow_steps (
    id          BIGINT      AUTO_INCREMENT PRIMARY KEY,
    workflow_id BIGINT      NOT NULL,
    name        VARCHAR(150) NOT NULL,
    description TEXT,
    order_index INT          NOT NULL,
    optional    BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_workflow_steps_workflow FOREIGN KEY (workflow_id) REFERENCES workflows(id) ON DELETE CASCADE
) ENGINE=InnoDB;
