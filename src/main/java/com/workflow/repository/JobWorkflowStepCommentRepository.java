package com.workflow.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.workflow.entity.JobWorkflowStepComment;

public interface JobWorkflowStepCommentRepository
        extends JpaRepository<JobWorkflowStepComment, Long> {

    List<JobWorkflowStepComment> findByStepIdOrderByCreatedAtAsc(Long stepId);
}
