package com.workflow.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.workflow.entity.JobWorkflowStepAttachment;

public interface JobWorkflowStepAttachmentRepository
        extends JpaRepository<JobWorkflowStepAttachment, Long> {

    List<JobWorkflowStepAttachment> findByStepIdOrderByCreatedAtAsc(Long stepId);
}
