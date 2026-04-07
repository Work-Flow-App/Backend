package com.workflow.repository.financial;

import com.workflow.entity.financial.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    @Query("SELECT DISTINCT i FROM Invoice i " +
           "JOIN FETCH i.estimate " +
           "JOIN FETCH i.company " +
           "LEFT JOIN FETCH i.lineItems " +
           "WHERE i.estimate.id = :estimateId AND i.company.id = :companyId")
    List<Invoice> findByEstimateIdAndCompanyId(@Param("estimateId") Long estimateId,
                                               @Param("companyId") Long companyId);

    Optional<Invoice> findByIdAndCompanyId(Long id, Long companyId);

    long countByCompanyId(Long companyId);

    boolean existsByLineItemsId(Long lineItemId);
}
