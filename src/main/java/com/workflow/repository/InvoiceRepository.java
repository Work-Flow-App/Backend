package com.workflow.repository;

import com.workflow.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByEstimateIdAndCompanyId(Long estimateId, Long companyId);
    Optional<Invoice> findByIdAndCompanyId(Long id, Long companyId);
    long countByCompanyId(Long companyId);
}
