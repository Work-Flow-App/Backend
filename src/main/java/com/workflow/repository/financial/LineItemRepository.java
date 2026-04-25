package com.workflow.repository.financial;

import com.workflow.entity.financial.LineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LineItemRepository extends JpaRepository<LineItem, Long> {
    List<LineItem> findByCompanyId(Long companyId);
    Optional<LineItem> findByIdAndCompanyId(Long id, Long companyId);

    @Modifying
    @Query("UPDATE LineItem l SET l.invoiced = true WHERE l.id IN :ids")
    void markAsInvoiced(@Param("ids") List<Long> ids);
}
