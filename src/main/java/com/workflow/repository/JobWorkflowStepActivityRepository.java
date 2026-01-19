package com.workflow.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.workflow.entity.JobWorkflowStepActivity;

public interface JobWorkflowStepActivityRepository
        extends JpaRepository<JobWorkflowStepActivity, Long> {

    List<JobWorkflowStepActivity> findByStepIdOrderByCreatedAtAsc(Long stepId);
}
