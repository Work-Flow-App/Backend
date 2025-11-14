package com.workflow.repository;

import com.workflow.entity.JobTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JobTemplateRepository extends JpaRepository<JobTemplate, Long> {
    List<JobTemplate> findByCompanyId(Long companyId);
}
