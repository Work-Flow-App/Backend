package com.workflow.repository.job;

import com.workflow.entity.job.JobWorkflowStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JobWorkflowStepRepository extends JpaRepository<JobWorkflowStep, Long> {
    void deleteByJobWorkflowId(Long jobWorkflowId);

    // Fetch all steps assigned to a specific worker (with eager-loaded workflow and job to avoid lazy chains)
    @Query("SELECT s FROM JobWorkflowStep s JOIN FETCH s.jobWorkflow jw JOIN FETCH jw.job WHERE :workerId MEMBER OF s.assignedWorkers")
    List<JobWorkflowStep> findByAssignedWorkers_Id(@Param("workerId") Long workerId);

    // Batch-load steps for multiple workflows at once; JOIN FETCH jobWorkflow so
    // callers can group by s.getJobWorkflow().getId() without triggering N+1 lazy loads
    @Query("SELECT s FROM JobWorkflowStep s JOIN FETCH s.jobWorkflow WHERE s.jobWorkflow.id IN :workflowIds ORDER BY s.orderIndex ASC")
    List<JobWorkflowStep> findByJobWorkflowIdInOrderByOrderIndexAsc(@Param("workflowIds") List<Long> workflowIds);

    // Fetch a specific step ensuring the worker is assigned to it
    @Query("SELECT s FROM JobWorkflowStep s JOIN s.assignedWorkers w WHERE s.id = :stepId AND w.id = :workerId")
    java.util.Optional<JobWorkflowStep> findByIdAndWorkerId(@Param("stepId") Long stepId,
            @Param("workerId") Long workerId);

    @Query("SELECT COALESCE(MAX(s.orderIndex), 0) FROM JobWorkflowStep s WHERE s.jobWorkflow.id = :jobWorkflowId")
    Integer findMaxOrderIndexByJobWorkflowId(@Param("jobWorkflowId") Long jobWorkflowId);

    @Query("SELECT COUNT(s) > 0 FROM JobWorkflowStep s JOIN s.assignedWorkers w WHERE s.jobWorkflow.id = :jwId AND w.id = :workerId")
    boolean existsByJobWorkflowIdAndWorkerId(@Param("jwId") Long jwId, @Param("workerId") Long workerId);

    @Query("SELECT DISTINCT s FROM JobWorkflowStep s LEFT JOIN FETCH s.assignedWorkers WHERE s.jobWorkflow.id = :jobWorkflowId ORDER BY s.orderIndex ASC")
    List<JobWorkflowStep> findByJobWorkflowIdOrderByOrderIndexAsc(@Param("jobWorkflowId") Long jobWorkflowId);

}
