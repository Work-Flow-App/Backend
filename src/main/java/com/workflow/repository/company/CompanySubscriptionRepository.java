package com.workflow.repository.company;

import com.workflow.common.constant.SubscriptionStatus;
import com.workflow.entity.company.CompanySubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CompanySubscriptionRepository extends JpaRepository<CompanySubscription, Long> {

    Optional<CompanySubscription> findByCompanyId(Long companyId);

    Optional<CompanySubscription> findByPaddleSubscriptionId(String paddleSubscriptionId);

    Optional<CompanySubscription> findByPaddleCustomerId(String paddleCustomerId);

    @Query("SELECT s FROM CompanySubscription s WHERE s.status = :status AND s.trialEndsAt < :now")
    List<CompanySubscription> findExpiredByStatus(
            @Param("status") SubscriptionStatus status,
            @Param("now") LocalDateTime now);

    @Query("SELECT s FROM CompanySubscription s WHERE s.status = 'PAST_DUE' AND s.currentPeriodEnd < :cutoff")
    List<CompanySubscription> findExpiredPastDue(@Param("cutoff") LocalDateTime cutoff);
}