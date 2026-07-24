package com.workflow.repository.form;

import com.workflow.entity.form.FormFieldValue;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FormFieldValueRepository extends JpaRepository<FormFieldValue, Long> {
    void deleteBySubmissionId(Long submissionId);
}