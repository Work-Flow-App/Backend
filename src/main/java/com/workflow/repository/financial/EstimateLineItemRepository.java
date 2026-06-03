package com.workflow.repository.financial;

import com.workflow.common.constant.financial.LineItemStatus;
import com.workflow.entity.financial.EstimateLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EstimateLineItemRepository extends JpaRepository<EstimateLineItem, Long> {

    Optional<EstimateLineItem> findByIdAndEstimateId(Long id, Long estimateId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE EstimateLineItem eli SET eli.status = :status WHERE eli.id IN :ids AND eli.status = com.workflow.common.constant.financial.LineItemStatus.AVAILABLE")
    void updateStatusForIdsIfAvailable(@Param("ids") List<Long> ids, @Param("status") LineItemStatus status);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE EstimateLineItem eli SET eli.status = com.workflow.common.constant.financial.LineItemStatus.INVOICED WHERE eli.id IN :ids")
    void markAsInvoiced(@Param("ids") List<Long> ids);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM EstimateLineItem eli WHERE eli.estimate.job.id = :jobId")
    void deleteByJobId(@Param("jobId") Long jobId);

    @Query("SELECT eli.id FROM EstimateLineItem eli " +
           "WHERE eli.id IN :ids " +
           "AND eli.status <> com.workflow.common.constant.financial.LineItemStatus.AVAILABLE")
    List<Long> findIdsWithNonAvailableStatus(@Param("ids") List<Long> ids);
}
