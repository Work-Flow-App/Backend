package com.workflow.repository.job;

import com.workflow.entity.job.JobTemplateField;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JobTemplateFieldRepository extends JpaRepository<JobTemplateField, Long> {
    List<JobTemplateField> findByTemplateIdOrderByOrderIndexAsc(Long templateId);
}
