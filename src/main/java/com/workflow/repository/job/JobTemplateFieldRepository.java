package com.workflow.repository.job;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.workflow.entity.JobTemplateField;

@Repository
public interface JobTemplateFieldRepository extends JpaRepository<JobTemplateField, Long> {
    List<JobTemplateField> findByTemplateIdOrderByOrderIndex(Long templateId);
}
