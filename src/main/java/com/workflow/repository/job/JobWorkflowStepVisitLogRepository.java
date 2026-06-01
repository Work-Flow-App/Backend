package com.workflow.repository.job;

import com.workflow.entity.job.JobWorkflowStepVisitLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JobWorkflowStepVisitLogRepository extends JpaRepository<JobWorkflowStepVisitLog, Long> {
    List<JobWorkflowStepVisitLog> findByStepIdOrderByVisitDateDescTimeInDesc(Long stepId);

    boolean existsByStepId(Long stepId);
}