package com.workflow.repository.job;

import com.workflow.entity.job.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {

       @Override
       @EntityGraph(attributePaths = { "company", "template", "customer", "client", "workflow", "address" })
       Page<Job> findAll(@Nullable Specification<Job> spec, Pageable pageable);

       boolean existsByWorkflowIdAndArchivedFalse(Long workflowId);

       @Query("SELECT DISTINCT j FROM Job j " +
                     "JOIN FETCH j.company " +
                     "JOIN FETCH j.template " +
                     "LEFT JOIN FETCH j.customer " +
                     "LEFT JOIN FETCH j.client " +
                     "LEFT JOIN FETCH j.workflow " +
                     "WHERE j.company.id = :companyId AND j.archived = false " +
                     "ORDER BY j.createdAt DESC")
       List<Job> findByCompanyId(@Param("companyId") Long companyId);

       @Query("SELECT DISTINCT j FROM Job j " +
                     "JOIN FETCH j.company " +
                     "JOIN FETCH j.template " +
                     "LEFT JOIN FETCH j.customer " +
                     "LEFT JOIN FETCH j.client " +
                     "LEFT JOIN FETCH j.workflow " +
                     "WHERE j.template.id = :templateId AND j.company.id = :companyId AND j.archived = false " +
                     "ORDER BY j.createdAt DESC")
       List<Job> findByTemplateIdAndCompanyId(@Param("templateId") Long templateId,
                     @Param("companyId") Long companyId);

       @Query("SELECT DISTINCT j FROM Job j " +
                     "JOIN FETCH j.company " +
                     "JOIN FETCH j.template " +
                     "LEFT JOIN FETCH j.customer " +
                     "LEFT JOIN FETCH j.client " +
                     "LEFT JOIN FETCH j.workflow " +
                     "WHERE j.company.id = :companyId AND j.archived = true " +
                     "ORDER BY j.createdAt DESC")
       List<Job> findArchivedByCompanyId(@Param("companyId") Long companyId);

       // Deliberately NOT filtered by archived=false — the cap is about job-creation load this
       // calendar month, not current active count. Filtering archived out would let a company
       // archive jobs to bypass the cap and keep creating more in the same month.
       @Query("SELECT COUNT(j) FROM Job j WHERE j.company.id = :companyId AND j.createdAt >= :start AND j.createdAt < :end")
       long countByCompanyIdAndCreatedAtBetween(
                     @Param("companyId") Long companyId,
                     @Param("start") LocalDateTime start,
                     @Param("end") LocalDateTime end);

       Optional<Job> findByJobRefAndCompanyId(Long jobRef, Long companyId);
}