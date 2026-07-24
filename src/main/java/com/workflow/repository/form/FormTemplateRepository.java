package com.workflow.repository.form;

import com.workflow.entity.form.FormTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FormTemplateRepository extends JpaRepository<FormTemplate, Long> {
    List<FormTemplate> findByCompanyIdAndArchivedFalse(Long companyId);

    Optional<FormTemplate> findByIdAndCompanyIdAndArchivedFalse(Long id, Long companyId);
}