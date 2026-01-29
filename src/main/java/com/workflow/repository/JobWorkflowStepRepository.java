package com.workflow.repository;

import com.workflow.entity.JobWorkflowStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JobWorkflowStepRepository extends JpaRepository<JobWorkflowStep, Long> {
    List<JobWorkflowStep> findByJobWorkflowIdOrderByOrderIndexAsc(Long jobWorkflowId);

    List<JobWorkflowStep> findByJobWorkflowId(Long jobWorkflowId);

    void deleteByJobWorkflowId(Long jobWorkflowId);

    // Fetch all steps assigned to a specific worker
    List<JobWorkflowStep> findByAssignedWorkers_Id(Long workerId);

    // Fetch a specific step ensuring the worker is assigned to it
    @Query("SELECT s FROM JobWorkflowStep s JOIN s.assignedWorkers w WHERE s.id = :stepId AND w.id = :workerId")
    java.util.Optional<JobWorkflowStep> findByIdAndWorkerId(@Param("stepId") Long stepId,
            @Param("workerId") Long workerId);

    @Query("SELECT COALESCE(MAX(s.orderIndex), 0) FROM JobWorkflowStep s WHERE s.jobWorkflow.id = :jobWorkflowId")
    Integer findMaxOrderIndexByJobWorkflowId(@Param("jobWorkflowId") Long jobWorkflowId);

}
