package com.workflow.repository.company;

import com.workflow.entity.company.CompanyPost;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CompanyPostRepository extends JpaRepository<CompanyPost, Long> {
    List<CompanyPost> findByCompanyIdOrderByCreatedAtDesc(Long companyId);

    List<CompanyPost> findByCompanyIdAndIsPublicTrueOrderByCreatedAtDesc(Long companyId);

    // Added for Group filtering
    List<CompanyPost> findByCompanyIdAndGroupIdOrderByCreatedAtDesc(Long companyId, Long groupId);

    List<CompanyPost> findByCompanyIdAndGroupIdAndIsPublicTrueOrderByCreatedAtDesc(Long companyId, Long groupId);

    // Check if group is in use
    boolean existsByGroupId(Long groupId);
}