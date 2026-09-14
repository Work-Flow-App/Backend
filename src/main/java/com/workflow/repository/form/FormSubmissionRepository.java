package com.workflow.repository.form;

import com.workflow.entity.form.FormSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FormSubmissionRepository extends JpaRepository<FormSubmission, Long> {
    List<FormSubmission> findByCompanyId(Long companyId);

    List<FormSubmission> findByWorkerId(Long workerId);

    Optional<FormSubmission> findByIdAndCompanyId(Long id, Long companyId);

    Optional<FormSubmission> findByIdAndWorkerId(Long id, Long workerId);

    List<FormSubmission> findByJobIdAndCompanyId(Long jobId, Long companyId);

    long countByTemplateId(Long templateId);
}