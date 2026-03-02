package com.workflow.repository;

import com.workflow.entity.JobWorkflowStepVisitLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JobWorkflowStepVisitLogRepository extends JpaRepository<JobWorkflowStepVisitLog, Long> {
    List<JobWorkflowStepVisitLog> findByStepIdOrderByVisitDateDescTimeInDesc(Long stepId);
}