package com.workflow.repository.financial;

import com.workflow.entity.financial.Estimate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EstimateRepository extends JpaRepository<Estimate, Long> {

    @Query("SELECT e FROM Estimate e " +
           "JOIN FETCH e.job " +
           "JOIN FETCH e.company " +
           "LEFT JOIN FETCH e.lineItems " +
           "WHERE e.job.id = :jobId AND e.company.id = :companyId")
    Optional<Estimate> findByJobIdAndCompanyId(@Param("jobId") Long jobId, @Param("companyId") Long companyId);

    @Query("SELECT e FROM Estimate e " +
           "JOIN FETCH e.job " +
           "JOIN FETCH e.company " +
           "LEFT JOIN FETCH e.lineItems " +
           "WHERE e.id = :id AND e.company.id = :companyId")
    Optional<Estimate> findByIdAndCompanyId(@Param("id") Long id, @Param("companyId") Long companyId);

    @Query("SELECT e FROM Estimate e " +
           "JOIN FETCH e.company " +
           "JOIN FETCH e.job j " +
           "JOIN FETCH j.customer " +
           "LEFT JOIN FETCH e.lineItems " +
           "WHERE e.id = :id AND e.company.id = :companyId")
    Optional<Estimate> findByIdWithDetailsAndCompanyId(@Param("id") Long id, @Param("companyId") Long companyId);

    boolean existsByJobId(Long jobId);
}
