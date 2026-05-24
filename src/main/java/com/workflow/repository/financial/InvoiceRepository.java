package com.workflow.repository.financial;

import com.workflow.entity.financial.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    @Query("SELECT DISTINCT i FROM Invoice i " +
           "JOIN FETCH i.estimate " +
           "JOIN FETCH i.company " +
           "LEFT JOIN FETCH i.lineItemSnapshots " +
           "WHERE i.estimate.id = :estimateId AND i.company.id = :companyId")
    List<Invoice> findByEstimateIdAndCompanyId(@Param("estimateId") Long estimateId,
                                               @Param("companyId") Long companyId);

    @Query("SELECT DISTINCT i FROM Invoice i " +
           "JOIN FETCH i.estimate " +
           "JOIN FETCH i.company " +
           "LEFT JOIN FETCH i.lineItemSnapshots " +
           "WHERE i.company.id = :companyId " +
           "ORDER BY i.id DESC")
    List<Invoice> findAllByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT i FROM Invoice i " +
           "JOIN FETCH i.estimate " +
           "JOIN FETCH i.company " +
           "LEFT JOIN FETCH i.lineItemSnapshots " +
           "WHERE i.id = :id AND i.company.id = :companyId")
    Optional<Invoice> findByIdAndCompanyId(@Param("id") Long id,
                                           @Param("companyId") Long companyId);

    List<Invoice> findByEstimateId(Long estimateId);

    long countByCompanyId(Long companyId);

    @Modifying(clearAutomatically = true)
    @Query("""
            DELETE FROM JobLineItemSnapshot s
            WHERE (s.invoice IS NOT NULL AND s.invoice.estimate.job.id = :jobId)
               OR (s.estimateDocument IS NOT NULL AND s.estimateDocument.estimate.job.id = :jobId)
            """)
    void deleteLineItemSnapshotsByJobId(@Param("jobId") Long jobId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Invoice i WHERE i.estimate.job.id = :jobId")
    void deleteByJobId(@Param("jobId") Long jobId);
}
