package com.workflow.repository.job;

import com.workflow.entity.job.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {

    boolean existsByWorkflowIdAndArchivedFalse(Long workflowId);

    @Query("SELECT DISTINCT j FROM Job j " +
           "JOIN FETCH j.company " +
           "JOIN FETCH j.template " +
           "LEFT JOIN FETCH j.customer " +
           "LEFT JOIN FETCH j.client " +
           "LEFT JOIN FETCH j.workflow " +
           "WHERE j.company.id = :companyId AND j.archived = false")
    List<Job> findByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT DISTINCT j FROM Job j " +
           "JOIN FETCH j.company " +
           "JOIN FETCH j.template " +
           "LEFT JOIN FETCH j.customer " +
           "LEFT JOIN FETCH j.client " +
           "LEFT JOIN FETCH j.workflow " +
           "WHERE j.template.id = :templateId AND j.company.id = :companyId AND j.archived = false")
    List<Job> findByTemplateIdAndCompanyId(@Param("templateId") Long templateId,
                                           @Param("companyId") Long companyId);

    @Query("SELECT DISTINCT j FROM Job j " +
           "JOIN FETCH j.company " +
           "JOIN FETCH j.template " +
           "LEFT JOIN FETCH j.customer " +
           "LEFT JOIN FETCH j.client " +
           "LEFT JOIN FETCH j.workflow " +
           "WHERE j.company.id = :companyId AND j.archived = true")
    List<Job> findArchivedByCompanyId(@Param("companyId") Long companyId);
}
