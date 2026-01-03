package com.workflow.repository;

import com.workflow.entity.JobWorkflow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobWorkflowRepository extends JpaRepository<JobWorkflow, Long> {
    Optional<JobWorkflow> findByJobId(Long jobId);

    List<JobWorkflow> findByJob_Company_Id(Long companyId);

    void deleteByJobId(Long jobId);
}
