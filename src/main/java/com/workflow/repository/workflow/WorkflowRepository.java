package com.workflow.repository.workflow;

import com.workflow.entity.workflow.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkflowRepository extends JpaRepository<Workflow, Long> {
    List<Workflow> findByCompanyIdAndArchivedFalse(Long companyId);
    boolean existsByCompanyIdAndName(Long companyId, String name);
    Optional<Workflow> findByIdAndCompanyId(Long id, Long companyId);
}
