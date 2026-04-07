package com.workflow.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.workflow.entity.JobWorkflowStepAttachment;

public interface JobWorkflowStepAttachmentRepository
        extends JpaRepository<JobWorkflowStepAttachment, Long> {

    @Query("SELECT a FROM JobWorkflowStepAttachment a JOIN FETCH a.uploadedBy WHERE a.step.id = :stepId ORDER BY a.createdAt ASC")
    List<JobWorkflowStepAttachment> findByStepIdOrderByCreatedAtAsc(@Param("stepId") Long stepId);
}
