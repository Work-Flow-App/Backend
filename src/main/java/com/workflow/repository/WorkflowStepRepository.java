package com.workflow.repository;

import com.workflow.entity.WorkflowStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WorkflowStepRepository extends JpaRepository<WorkflowStep, Long> {
    List<WorkflowStep> findByWorkflowIdOrderByOrderIndexAsc(Long workflowId);

    @Query("SELECT s FROM WorkflowStep s JOIN FETCH s.workflow w WHERE w.company.id = :companyId ORDER BY w.id ASC, s.orderIndex ASC")
    List<WorkflowStep> findByWorkflow_Company_IdOrderByWorkflow_IdAscOrderIndexAsc(@Param("companyId") Long companyId);


    Optional<WorkflowStep> findByIdAndWorkflow_CompanyId(Long id, Long companyId);
}
