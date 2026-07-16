package com.workflow.repository.job;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.workflow.entity.job.JobWorkflowStepVisitLog;

public interface JobWorkflowStepVisitLogRepository extends JpaRepository<JobWorkflowStepVisitLog, Long> {
    List<JobWorkflowStepVisitLog> findByStepIdOrderByVisitDateDescTimeInDesc(Long stepId);

    boolean existsByStepId(Long stepId);

    @Query("SELECT v FROM JobWorkflowStepVisitLog v WHERE v.loggedBy.id = :userId " +
            "AND v.visitDate BETWEEN :start AND :end")
    List<JobWorkflowStepVisitLog> findByLoggedByIdAndVisitDateBetween(
            @Param("userId") Long userId, @Param("start") LocalDate start, @Param("end") LocalDate end);
}
