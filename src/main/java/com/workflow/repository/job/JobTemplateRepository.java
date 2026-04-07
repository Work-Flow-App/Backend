package com.workflow.repository.job;

import com.workflow.entity.job.JobTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JobTemplateRepository extends JpaRepository<JobTemplate, Long> {
    List<JobTemplate> findByCompanyId(Long companyId);
    boolean existsByCompanyIdAndName(Long companyId, String name);

    // Find current default template for a company
    Optional<JobTemplate> findByCompanyIdAndIsDefaultTrue(Long companyId);

    // Clear default flag for all templates in a company (used when setting new default)
    @Modifying
    @Query("UPDATE JobTemplate t SET t.isDefault = false WHERE t.company.id = :companyId AND t.isDefault = true")
    void clearDefaultForCompany(@Param("companyId") Long companyId);
}
