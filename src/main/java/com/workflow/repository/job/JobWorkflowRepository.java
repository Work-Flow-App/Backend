package com.workflow.repository.job;

import com.workflow.entity.job.JobWorkflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JobWorkflowRepository extends JpaRepository<JobWorkflow, Long> {
    Optional<JobWorkflow> findByJobId(Long jobId);

    @Query("SELECT jw FROM JobWorkflow jw JOIN FETCH jw.job WHERE jw.job.company.id = :companyId")
    List<JobWorkflow> findByJob_Company_Id(@Param("companyId") Long companyId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM JobWorkflow jw WHERE jw.job.id = :jobId")
    void deleteByJobId(@Param("jobId") Long jobId);
}
