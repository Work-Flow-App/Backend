package com.workflow.repository.financial;

import com.workflow.entity.financial.EstimateDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EstimateDocumentRepository extends JpaRepository<EstimateDocument, Long> {

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM EstimateDocument d WHERE d.estimate.job.id = :jobId")
    void deleteByEstimateJobId(@Param("jobId") Long jobId);

    @Query("SELECT d FROM EstimateDocument d " +
           "JOIN FETCH d.estimate " +
           "JOIN FETCH d.company " +
           "LEFT JOIN FETCH d.lineItemSnapshots " +
           "WHERE d.id = :id AND d.company.id = :companyId")
    Optional<EstimateDocument> findByIdAndCompanyId(@Param("id") Long id,
                                                    @Param("companyId") Long companyId);

    @Query("SELECT DISTINCT d FROM EstimateDocument d " +
           "JOIN FETCH d.estimate " +
           "JOIN FETCH d.company " +
           "LEFT JOIN FETCH d.lineItemSnapshots " +
           "WHERE d.estimate.id = :estimateId AND d.company.id = :companyId " +
           "ORDER BY d.id DESC")
    List<EstimateDocument> findByEstimateIdAndCompanyId(@Param("estimateId") Long estimateId,
                                                        @Param("companyId") Long companyId);
}
