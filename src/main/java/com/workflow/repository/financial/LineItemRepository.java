package com.workflow.repository.financial;

import com.workflow.entity.financial.LineItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LineItemRepository extends JpaRepository<LineItem, Long> {
    List<LineItem> findByCompanyId(Long companyId);
    Optional<LineItem> findByIdAndCompanyId(Long id, Long companyId);
}
