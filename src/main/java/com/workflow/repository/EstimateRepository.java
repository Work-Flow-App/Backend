package com.workflow.repository;

import com.workflow.entity.Estimate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EstimateRepository extends JpaRepository<Estimate, Long> {
    Optional<Estimate> findByJobIdAndCompanyId(Long jobId, Long companyId);
    Optional<Estimate> findByIdAndCompanyId(Long id, Long companyId);
    boolean existsByJobId(Long jobId);
}
