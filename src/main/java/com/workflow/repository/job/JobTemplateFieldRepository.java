package com.workflow.repository.job;

import com.workflow.entity.job.JobTemplateField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JobTemplateFieldRepository extends JpaRepository<JobTemplateField, Long> {
    List<JobTemplateField> findByTemplateIdOrderByOrderIndexAsc(Long templateId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM JobTemplateField f WHERE f.template.id = :templateId")
    void deleteByTemplateId(@Param("templateId") Long templateId);
}
