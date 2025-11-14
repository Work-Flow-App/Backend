package com.workflow.repository;

import com.workflow.entity.JobTemplateField;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JobTemplateFieldRepository extends JpaRepository<JobTemplateField, Long> {
    List<JobTemplateField> findByTemplateIdOrderByOrderIndexAsc(Long templateId);
}
