package com.workflow.repository.company;

import com.workflow.common.constant.SubscriptionStatus;
import com.workflow.entity.company.CompanySubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CompanySubscriptionRepository extends JpaRepository<CompanySubscription, Long> {

    Optional<CompanySubscription> findByCompanyId(Long companyId);

    Optional<CompanySubscription> findByPaddleSubscriptionId(String paddleSubscriptionId);

    Optional<CompanySubscription> findByPaddleCustomerId(String paddleCustomerId);

    @Modifying
    @Query("UPDATE CompanySubscription s SET " +
           "s.paddleSubscriptionId = CASE WHEN :subId IS NOT NULL THEN :subId ELSE s.paddleSubscriptionId END, " +
           "s.paddleCustomerId = CASE WHEN :custId IS NOT NULL THEN :custId ELSE s.paddleCustomerId END " +
           "WHERE s.id = :id")
    void updatePaddleIds(@Param("id") Long id, @Param("subId") String subId, @Param("custId") String custId);

    /**
     * Used by subscription.activated: atomically sets status, currentPeriodEnd, and lastEventOccurredAt.
     * The WHERE clause on lastEventOccurredAt prevents out-of-order events from rolling back state.
     * Returns the number of rows updated (0 = stale event, no-op).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE CompanySubscription s SET " +
           "s.status = :status, " +
           "s.currentPeriodEnd = :periodEnd, " +
           "s.lastEventOccurredAt = :occurredAt " +
           "WHERE s.id = :id " +
           "AND (s.lastEventOccurredAt IS NULL OR s.lastEventOccurredAt < :occurredAt)")
    int updateActivated(
            @Param("id") Long id,
            @Param("status") SubscriptionStatus status,
            @Param("periodEnd") LocalDateTime periodEnd,
            @Param("occurredAt") LocalDateTime occurredAt);

    /**
     * Used by subscription.past_due, subscription.paused, subscription.cancelled, subscription.resumed:
     * atomically sets status and lastEventOccurredAt. Does not touch currentPeriodEnd.
     * Returns the number of rows updated (0 = stale event, no-op).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE CompanySubscription s SET " +
           "s.status = :status, " +
           "s.lastEventOccurredAt = :occurredAt " +
           "WHERE s.id = :id " +
           "AND (s.lastEventOccurredAt IS NULL OR s.lastEventOccurredAt < :occurredAt)")
    int updateStatus(
            @Param("id") Long id,
            @Param("status") SubscriptionStatus status,
            @Param("occurredAt") LocalDateTime occurredAt);

    /**
     * Used by subscription.updated when both a mapped status and a periodEnd are present.
     * Returns the number of rows updated (0 = stale event, no-op).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE CompanySubscription s SET " +
           "s.status = :status, " +
           "s.currentPeriodEnd = :periodEnd, " +
           "s.lastEventOccurredAt = :occurredAt " +
           "WHERE s.id = :id " +
           "AND (s.lastEventOccurredAt IS NULL OR s.lastEventOccurredAt < :occurredAt)")
    int updateStatusAndPeriod(
            @Param("id") Long id,
            @Param("status") SubscriptionStatus status,
            @Param("periodEnd") LocalDateTime periodEnd,
            @Param("occurredAt") LocalDateTime occurredAt);

    /**
     * Used by subscription.updated when only a mapped status is present (no periodEnd).
     * Returns the number of rows updated (0 = stale event, no-op).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE CompanySubscription s SET " +
           "s.status = :status, " +
           "s.lastEventOccurredAt = :occurredAt " +
           "WHERE s.id = :id " +
           "AND (s.lastEventOccurredAt IS NULL OR s.lastEventOccurredAt < :occurredAt)")
    int updateStatusOnly(
            @Param("id") Long id,
            @Param("status") SubscriptionStatus status,
            @Param("occurredAt") LocalDateTime occurredAt);

    /**
     * Used by subscription.updated when only a periodEnd is present (unknown/unmapped Paddle status).
     * Returns the number of rows updated (0 = stale event, no-op).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE CompanySubscription s SET " +
           "s.currentPeriodEnd = :periodEnd, " +
           "s.lastEventOccurredAt = :occurredAt " +
           "WHERE s.id = :id " +
           "AND (s.lastEventOccurredAt IS NULL OR s.lastEventOccurredAt < :occurredAt)")
    int updatePeriodOnly(
            @Param("id") Long id,
            @Param("periodEnd") LocalDateTime periodEnd,
            @Param("occurredAt") LocalDateTime occurredAt);

    /**
     * Used by subscription.updated when neither status nor periodEnd are actionable.
     * Still advances lastEventOccurredAt to mark this event as processed.
     * Returns the number of rows updated (0 = stale event, no-op).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE CompanySubscription s SET " +
           "s.lastEventOccurredAt = :occurredAt " +
           "WHERE s.id = :id " +
           "AND (s.lastEventOccurredAt IS NULL OR s.lastEventOccurredAt < :occurredAt)")
    int updateTimestampOnly(
            @Param("id") Long id,
            @Param("occurredAt") LocalDateTime occurredAt);

    @Query("SELECT s FROM CompanySubscription s WHERE s.status = :status AND s.trialEndsAt < :now")
    List<CompanySubscription> findExpiredByStatus(
            @Param("status") SubscriptionStatus status,
            @Param("now") LocalDateTime now);

    @Query("SELECT s FROM CompanySubscription s WHERE s.status = 'PAST_DUE' AND s.currentPeriodEnd < :cutoff")
    List<CompanySubscription> findExpiredPastDue(@Param("cutoff") LocalDateTime cutoff);
}