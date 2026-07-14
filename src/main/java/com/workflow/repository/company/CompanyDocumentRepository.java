package com.workflow.repository.company;

import com.workflow.entity.company.CompanyDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CompanyDocumentRepository extends JpaRepository<CompanyDocument, Long> {
    List<CompanyDocument> findByCompanyIdOrderByCreatedAtDesc(Long companyId);
    List<CompanyDocument> findByCompanyIdAndIsPublicTrueOrderByCreatedAtDesc(Long companyId);
}