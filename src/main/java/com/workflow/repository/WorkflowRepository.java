package com.workflow.repository;

import com.workflow.entity.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkflowRepository extends JpaRepository<Workflow, Long> {
    List<Workflow> findByCompanyId(Long companyId);
    boolean existsByCompanyIdAndName(Long companyId, String name);
}
