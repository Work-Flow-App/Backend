package com.workflow.repository.job;

import com.workflow.entity.job.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByCompanyId(Long companyId);
    List<Job> findByTemplateIdAndCompanyId(Long templateId, Long companyId);
}
