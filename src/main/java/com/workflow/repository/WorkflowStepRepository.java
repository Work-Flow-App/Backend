package com.workflow.repository;

import com.workflow.entity.WorkflowStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkflowStepRepository extends JpaRepository<WorkflowStep, Long> {
    List<WorkflowStep> findByWorkflowIdOrderByOrderIndexAsc(Long workflowId);

    List<WorkflowStep> findByWorkflow_Company_IdOrderByWorkflow_IdAscOrderIndexAsc(Long companyId);

}
