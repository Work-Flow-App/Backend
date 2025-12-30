package com.workflow.repository;

import com.workflow.entity.JobWorkflow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JobWorkflowRepository extends JpaRepository<JobWorkflow, Long> {
    Optional<JobWorkflow> findByJobId(Long jobId);

    void deleteByJobId(Long jobId);
}
