package com.workflow.service.paddle;

import com.fasterxml.jackson.databind.JsonNode;
import com.workflow.common.constant.SubscriptionStatus;
import com.workflow.dto.paddle.PaddleWebhookPayload;
import com.workflow.entity.company.CompanySubscription;
import com.workflow.entity.company.PaddleWebhookEvent;
import com.workflow.repository.company.CompanySubscriptionRepository;
import com.workflow.repository.company.PaddleWebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaddleWebhookHandler {

    private final CompanySubscriptionRepository subscriptionRepository;
    private final PaddleWebhookEventRepository webhookEventRepository;

    @Transactional
    public void handle(PaddleWebhookPayload payload) {
        String eventId = payload.eventId();
        String eventType = payload.eventType();

        // Idempotency guard — if we've already processed this event, skip.
        // Note: existsById + save is not atomic. Two concurrent deliveries of the same eventId
        // can both pass the existsById check. The unique PK on paddle_webhook_events catches this
        // at the DB level. We catch DataIntegrityViolationException below and treat it as a duplicate.
        if (webhookEventRepository.existsById(eventId)) {
            log.info("Duplicate Paddle webhook event ignored: eventId={}, eventType={}", eventId, eventType);
            return;
        }

        log.info("Processing Paddle webhook event: eventId={}, eventType={}", eventId, eventType);

        LocalDateTime occurredAt = parseOccurredAt(payload.occurredAt());
        JsonNode data = payload.data();

        switch (eventType) {
            case "subscription.created"   -> handleSubscriptionCreated(data, occurredAt);
            case "subscription.activated" -> handleSubscriptionActivated(data, occurredAt);
            case "subscription.updated"   -> handleSubscriptionUpdated(data, occurredAt);
            case "subscription.past_due"  -> handleSubscriptionPastDue(data, occurredAt);
            case "subscription.paused"    -> handleSubscriptionPaused(data, occurredAt);
            case "subscription.cancelled" -> handleSubscriptionCancelled(data, occurredAt);
            case "subscription.resumed"   -> handleSubscriptionResumed(data, occurredAt);
            default -> log.debug("Unhandled Paddle event type: {}", eventType);
        }

        // Save idempotency record AFTER processing — if processing throws, this won't be saved
        // and Paddle can safely retry. DataIntegrityViolationException on the PK means a concurrent
        // delivery won the race — safe to ignore.
        try {
            webhookEventRepository.save(PaddleWebhookEvent.builder()
                    .eventId(eventId)
                    .eventType(eventType)
                    .build());
        } catch (DataIntegrityViolationException e) {
            log.info("Concurrent duplicate Paddle webhook event detected, race resolved by DB constraint: eventId={}", eventId);
            return;
        }

        log.info("Paddle webhook event processed and recorded: eventId={}", eventId);
    }

    private void handleSubscriptionCreated(JsonNode data, LocalDateTime occurredAt) {
        Optional<CompanySubscription> subOpt = resolveSubscription(data);
        if (subOpt.isEmpty()) {
            log.warn("Could not resolve subscription for subscription.created event. data={}", data);
            return;
        }
        CompanySubscription sub = subOpt.get();
        String paddleSubId = data.path("id").asText(null);
        String paddleCustomerId = data.path("customer_id").asText(null);
        subscriptionRepository.updatePaddleIds(sub.getId(), paddleSubId, paddleCustomerId);
        log.info("subscription.created: companyId={}, paddleSubId={}", sub.getCompany().getId(), paddleSubId);
    }

    private void handleSubscriptionActivated(JsonNode data, LocalDateTime occurredAt) {
        resolveSubscription(data).ifPresentOrElse(sub -> {
            int rows = subscriptionRepository.updateActivated(
                    sub.getId(), SubscriptionStatus.ACTIVE, extractPeriodEnd(data), occurredAt);
            if (rows == 0) {
                log.debug("Skipping stale subscription.activated event for sub={}", sub.getPaddleSubscriptionId());
            } else {
                log.info("subscription.activated: companyId={}", sub.getCompany().getId());
            }
        }, () -> log.warn("Could not resolve subscription for subscription.activated"));
    }

    private void handleSubscriptionUpdated(JsonNode data, LocalDateTime occurredAt) {
        resolveSubscription(data).ifPresentOrElse(sub -> {
            String paddleStatus = data.path("status").asText(null);
            SubscriptionStatus mapped = (paddleStatus != null) ? mapPaddleStatus(paddleStatus) : null;
            LocalDateTime periodEnd = extractPeriodEnd(data);

            int rows;
            if (mapped != null && periodEnd != null) {
                rows = subscriptionRepository.updateStatusAndPeriod(sub.getId(), mapped, periodEnd, occurredAt);
            } else if (mapped != null) {
                rows = subscriptionRepository.updateStatusOnly(sub.getId(), mapped, occurredAt);
            } else if (periodEnd != null) {
                rows = subscriptionRepository.updatePeriodOnly(sub.getId(), periodEnd, occurredAt);
            } else {
                // Neither status nor periodEnd is actionable — advance the watermark only.
                // Happens when Paddle sends an update with an unmapped status and no billing period.
                log.warn("subscription.updated: no actionable fields — advancing timestamp only for sub={}",
                        sub.getPaddleSubscriptionId());
                rows = subscriptionRepository.updateTimestampOnly(sub.getId(), occurredAt);
            }

            if (rows == 0) {
                log.debug("Skipping stale subscription.updated event for sub={}", sub.getPaddleSubscriptionId());
            } else {
                log.info("subscription.updated: companyId={}, mappedStatus={}, periodEnd={}",
                        sub.getCompany().getId(), mapped, periodEnd);
            }
        }, () -> log.warn("Could not resolve subscription for subscription.updated"));
    }

    private void handleSubscriptionPastDue(JsonNode data, LocalDateTime occurredAt) {
        resolveSubscription(data).ifPresentOrElse(sub -> {
            int rows = subscriptionRepository.updateStatus(sub.getId(), SubscriptionStatus.PAST_DUE, occurredAt);
            if (rows == 0) {
                log.debug("Skipping stale subscription.past_due event for sub={}", sub.getPaddleSubscriptionId());
            } else {
                log.info("subscription.past_due: companyId={}", sub.getCompany().getId());
            }
        }, () -> log.warn("Could not resolve subscription for subscription.past_due"));
    }

    private void handleSubscriptionPaused(JsonNode data, LocalDateTime occurredAt) {
        resolveSubscription(data).ifPresentOrElse(sub -> {
            int rows = subscriptionRepository.updateStatus(sub.getId(), SubscriptionStatus.PAUSED, occurredAt);
            if (rows == 0) {
                log.debug("Skipping stale subscription.paused event for sub={}", sub.getPaddleSubscriptionId());
            } else {
                log.info("subscription.paused: companyId={}", sub.getCompany().getId());
            }
        }, () -> log.warn("Could not resolve subscription for subscription.paused"));
    }

    private void handleSubscriptionCancelled(JsonNode data, LocalDateTime occurredAt) {
        resolveSubscription(data).ifPresentOrElse(sub -> {
            int rows = subscriptionRepository.updateStatus(sub.getId(), SubscriptionStatus.CANCELLED, occurredAt);
            if (rows == 0) {
                log.debug("Skipping stale subscription.cancelled event for sub={}", sub.getPaddleSubscriptionId());
            } else {
                log.info("subscription.cancelled: companyId={}", sub.getCompany().getId());
            }
        }, () -> log.warn("Could not resolve subscription for subscription.cancelled"));
    }

    /**
     * subscription.resumed: set status back to ACTIVE using updateStatus (not updateActivated) so we
     * never null out an existing currentPeriodEnd when Paddle omits the billing period on resume.
     * If Paddle does include a new billing period, apply it with a separate updatePeriodOnly call —
     * no second staleness check needed because the updateStatus call above already proved this event is newer.
     */
    private void handleSubscriptionResumed(JsonNode data, LocalDateTime occurredAt) {
        resolveSubscription(data).ifPresentOrElse(sub -> {
            int rows = subscriptionRepository.updateStatus(sub.getId(), SubscriptionStatus.ACTIVE, occurredAt);
            if (rows == 0) {
                log.debug("Skipping stale subscription.resumed event for sub={}", sub.getPaddleSubscriptionId());
                return;
            }
            LocalDateTime periodEnd = extractPeriodEnd(data);
            if (periodEnd != null) {
                subscriptionRepository.updatePeriodOnly(sub.getId(), periodEnd, occurredAt);
            }
            log.info("subscription.resumed: companyId={}", sub.getCompany().getId());
        }, () -> log.warn("Could not resolve subscription for subscription.resumed"));
    }

    /**
     * Resolves the CompanySubscription from the webhook payload data using a priority chain:
     * 1. custom_data.companyId — most reliable, set by us when creating the checkout
     * 2. data.id (paddle subscription ID) — present on all subscription events
     * 3. data.customer_id — fallback for events where subscription ID is not yet stored
     */
    private Optional<CompanySubscription> resolveSubscription(JsonNode data) {
        String companyIdStr = data.path("custom_data").path("companyId").asText(null);
        if (companyIdStr != null && !companyIdStr.isBlank()) {
            try {
                Long companyId = Long.parseLong(companyIdStr);
                Optional<CompanySubscription> sub = subscriptionRepository.findByCompanyId(companyId);
                if (sub.isPresent()) return sub;
            } catch (NumberFormatException e) {
                log.warn("custom_data.companyId is not a valid Long: {}", companyIdStr);
            }
        }
        String paddleSubId = data.path("id").asText(null);
        if (paddleSubId != null && !paddleSubId.isBlank()) {
            Optional<CompanySubscription> sub = subscriptionRepository.findByPaddleSubscriptionId(paddleSubId);
            if (sub.isPresent()) return sub;
        }
        String paddleCustomerId = data.path("customer_id").asText(null);
        if (paddleCustomerId != null && !paddleCustomerId.isBlank()) {
            return subscriptionRepository.findByPaddleCustomerId(paddleCustomerId);
        }
        return Optional.empty();
    }

    /**
     * Maps Paddle's subscription status strings to our internal SubscriptionStatus enum.
     * Returns null for unknown statuses — callers must guard and skip the update rather than
     * corrupting subscription state with a wrong default.
     */
    private SubscriptionStatus mapPaddleStatus(String paddleStatus) {
        return switch (paddleStatus) {
            case "active"    -> SubscriptionStatus.ACTIVE;
            case "past_due"  -> SubscriptionStatus.PAST_DUE;
            case "paused"    -> SubscriptionStatus.PAUSED;
            case "cancelled" -> SubscriptionStatus.CANCELLED;
            default -> {
                log.warn("Unknown Paddle subscription status '{}' — skipping status update to avoid state corruption",
                        paddleStatus);
                yield null;
            }
        };
    }

    private LocalDateTime extractPeriodEnd(JsonNode data) {
        String endsAt = data.path("current_billing_period").path("ends_at").asText(null);
        if (endsAt == null || endsAt.isBlank()) return null;
        try {
            return OffsetDateTime.parse(endsAt).toLocalDateTime();
        } catch (Exception e) {
            log.warn("Could not parse billing period ends_at: {}", endsAt);
            return null;
        }
    }

    private LocalDateTime parseOccurredAt(String occurredAt) {
        if (occurredAt == null) return LocalDateTime.now();
        try {
            return OffsetDateTime.parse(occurredAt).toLocalDateTime();
        } catch (Exception e) {
            log.warn("Could not parse occurredAt: {}, using now", occurredAt);
            return LocalDateTime.now();
        }
    }
}
