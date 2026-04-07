package com.workflow.repository.job;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.workflow.entity.job.JobWorkflowStepComment;

public interface JobWorkflowStepCommentRepository
        extends JpaRepository<JobWorkflowStepComment, Long> {

    @Query("SELECT c FROM JobWorkflowStepComment c JOIN FETCH c.author WHERE c.step.id = :stepId ORDER BY c.createdAt ASC")
    List<JobWorkflowStepComment> findByStepIdOrderByCreatedAtAsc(@Param("stepId") Long stepId);
}
