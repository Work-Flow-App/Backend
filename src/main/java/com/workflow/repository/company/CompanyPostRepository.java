package com.workflow.repository.company;

import com.workflow.entity.company.CompanyPost;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CompanyPostRepository extends JpaRepository<CompanyPost, Long> {
    List<CompanyPost> findByCompanyIdOrderByCreatedAtDesc(Long companyId);
    List<CompanyPost> findByCompanyIdAndIsPublicTrueOrderByCreatedAtDesc(Long companyId);
}