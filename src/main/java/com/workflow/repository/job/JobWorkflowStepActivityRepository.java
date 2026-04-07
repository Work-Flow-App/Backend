package com.workflow.repository.job;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.workflow.entity.job.JobWorkflowStepActivity;

public interface JobWorkflowStepActivityRepository
        extends JpaRepository<JobWorkflowStepActivity, Long> {

    @Query("SELECT a FROM JobWorkflowStepActivity a JOIN FETCH a.actor WHERE a.step.id = :stepId ORDER BY a.createdAt ASC")
    List<JobWorkflowStepActivity> findByStepIdOrderByCreatedAtAsc(@Param("stepId") Long stepId);
}
