package com.workflow.repository;

import com.workflow.entity.JobWorkflowStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobWorkflowStepRepository extends JpaRepository<JobWorkflowStep, Long> {
    List<JobWorkflowStep> findByJobWorkflowIdOrderByOrderIndexAsc(Long jobWorkflowId);

    List<JobWorkflowStep> findByJobWorkflowId(Long jobWorkflowId);

    void deleteByJobWorkflowId(Long jobWorkflowId);
}
