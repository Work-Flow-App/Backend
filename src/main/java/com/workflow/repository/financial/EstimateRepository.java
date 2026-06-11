package com.workflow.repository.financial;

import com.workflow.entity.financial.Estimate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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
           "LEFT JOIN FETCH j.customer " +
           "LEFT JOIN FETCH e.lineItems " +
           "WHERE e.id = :id AND e.company.id = :companyId")
    Optional<Estimate> findByIdWithDetailsAndCompanyId(@Param("id") Long id, @Param("companyId") Long companyId);

    @Query("SELECT e.job.id, e.id, COALESCE(SUM(li.netAmount), 0) FROM Estimate e LEFT JOIN e.lineItems li WHERE e.job.id IN :jobIds GROUP BY e.job.id, e.id")
    List<Object[]> findEstimateSummaryByJobIds(@Param("jobIds") List<Long> jobIds);

    boolean existsByJobId(Long jobId);

    Optional<Estimate> findByJobId(Long jobId);

    @Query(value = """
            SELECT COALESCE(SUM(eli.total_amount), 0)
            FROM estimate_line_items eli
            JOIN estimates e ON e.id = eli.estimate_id
            JOIN jobs j ON j.id = e.job_id
            WHERE e.company_id = :companyId AND eli.status = 'WAITING_APPROVAL' AND j.archived = false AND j.status NOT IN ('COMPLETED', 'CANCELLED')
            """, nativeQuery = true)
    java.math.BigDecimal sumWaitingApprovalByCompanyId(@Param("companyId") Long companyId);

    @Query(value = """
            SELECT COALESCE(SUM(eli.total_amount), 0)
            FROM estimate_line_items eli
            JOIN estimates e ON e.id = eli.estimate_id
            JOIN jobs j ON j.id = e.job_id
            WHERE e.company_id = :companyId AND eli.status = 'APPROVED' AND j.archived = false AND j.status NOT IN ('COMPLETED', 'CANCELLED')
            """, nativeQuery = true)
    java.math.BigDecimal sumApprovedByCompanyId(@Param("companyId") Long companyId);

    @Query(value = """
            SELECT COALESCE(SUM(eli.total_amount), 0)
            FROM estimate_line_items eli
            JOIN estimates e ON e.id = eli.estimate_id
            JOIN jobs j ON j.id = e.job_id
            WHERE e.company_id = :companyId AND eli.status = 'INVOICED' AND j.archived = false AND j.status NOT IN ('COMPLETED', 'CANCELLED')
            """, nativeQuery = true)
    java.math.BigDecimal sumInvoicedByCompanyId(@Param("companyId") Long companyId);

    @Query(value = """
            SELECT COALESCE(SUM(eli.total_amount), 0)
            FROM estimate_line_items eli
            JOIN estimates e ON e.id = eli.estimate_id
            WHERE e.company_id = :companyId AND eli.status = 'INVOICED'
            """, nativeQuery = true)
    java.math.BigDecimal sumAllTimeInvoicedByCompanyId(@Param("companyId") Long companyId);
}
