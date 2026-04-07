package com.workflow.repository;

import com.workflow.entity.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkflowRepository extends JpaRepository<Workflow, Long> {
    List<Workflow> findByCompanyId(Long companyId);
    boolean existsByCompanyIdAndName(Long companyId, String name);
    Optional<Workflow> findByIdAndCompanyId(Long id, Long companyId);
}
