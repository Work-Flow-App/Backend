package com.workflow.repository.company;

import com.workflow.entity.company.CompanyPostGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CompanyPostGroupRepository extends JpaRepository<CompanyPostGroup, Long> {
    List<CompanyPostGroup> findByCompanyIdOrderByNameAsc(Long companyId);

    Optional<CompanyPostGroup> findByIdAndCompanyId(Long id, Long companyId);

    boolean existsByCompanyIdAndNameIgnoreCase(Long companyId, String name);
}