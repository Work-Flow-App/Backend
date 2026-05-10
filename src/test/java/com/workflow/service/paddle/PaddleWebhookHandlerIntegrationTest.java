package com.workflow.service.paddle;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.common.constant.Role;
import com.workflow.common.constant.SubscriptionStatus;
import com.workflow.dto.paddle.PaddleWebhookPayload;
import com.workflow.entity.auth.User;
import com.workflow.entity.company.Company;
import com.workflow.entity.company.CompanySubscription;
import com.workflow.entity.company.PaddleWebhookEvent;
import com.workflow.repository.auth.UserRepository;
import com.workflow.repository.company.CompanyRepository;
import com.workflow.repository.company.CompanySubscriptionRepository;
import com.workflow.repository.company.PaddleWebhookEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for PaddleWebhookHandler.
 *
 * Each test runs in its own transaction that is rolled back after the test.
 * IMPORTANT: @Modifying queries bypass the JPA first-level cache, so we must
 * re-fetch from the repository to assert the final DB state rather than reading
 * from a cached entity reference.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PaddleWebhookHandlerIntegrationTest {

    @Autowired private PaddleWebhookHandler handler;
    @Autowired private CompanySubscriptionRepository subscriptionRepository;
    @Autowired private PaddleWebhookEventRepository webhookEventRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private final ObjectMapper om = new ObjectMapper();

    // Fixed ISO timestamps used across tests
    private static final String T_MINUS_5  = "2024-01-01T05:00:00+00:00";
    private static final String T_PLUS_10  = "2024-01-01T10:00:00+00:00";
    private static final String T_PLUS_15  = "2024-01-01T15:00:00+00:00";
    private static final String PERIOD_END = "2024-02-01T00:00:00+00:00";

    private CompanySubscription subscription;
    private Long subscriptionId;

    @BeforeEach
    void setUp() {
        // H2 create-drop resets on context load; deleteAll in @BeforeEach ensures isolation
        // between tests that may insert rows outside the test's own transaction (none here,
        // since we're @Transactional, but it's defensive).
        webhookEventRepository.deleteAll();
        subscriptionRepository.deleteAll();
        companyRepository.deleteAll();
        userRepository.deleteAll();

        User user = userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString())
                .username("owner_" + UUID.randomUUID())
                .password(passwordEncoder.encode("pass"))
                .email(UUID.randomUUID() + "@test.com")
                .role(Role.COMPANY)
                .enabled(true)
                .build());

        Company company = companyRepository.save(Company.builder()
                .name("Acme Corp")
                .email("acme@test.com")
                .user(user)
                .archived(false)
                .build());

        subscription = subscriptionRepository.save(CompanySubscription.builder()
                .company(company)
                .trialEndsAt(LocalDateTime.now().plusDays(14))
                .status(SubscriptionStatus.TRIAL)
                .paddleSubscriptionId("sub_integ_123")
                .paddleCustomerId("cust_integ_999")
                .build());

        subscriptionId = subscription.getId();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private ObjectNode baseData() {
        ObjectNode data = om.createObjectNode();
        data.put("id", "sub_integ_123");
        data.put("customer_id", "cust_integ_999");
        ObjectNode customData = om.createObjectNode();
        customData.put("companyId", String.valueOf(subscription.getCompany().getId()));
        data.set("custom_data", customData);
        return data;
    }

    private void addBillingPeriod(ObjectNode data, String endsAt) {
        ObjectNode period = om.createObjectNode();
        period.put("ends_at", endsAt);
        data.set("current_billing_period", period);
    }

    private PaddleWebhookPayload payload(String eventId, String eventType, String occurredAt, ObjectNode data) {
        return new PaddleWebhookPayload(null, eventId, eventType, occurredAt, data);
    }

    private CompanySubscription reloadSubscription() {
        return subscriptionRepository.findById(subscriptionId).orElseThrow();
    }

    // -------------------------------------------------------------------------
    // Test 1: Staleness regression
    // Process T+10 event, then replay with T+5 — DB must retain the T+10 state.
    // -------------------------------------------------------------------------

    @Test
    void stalenessRegression_olderEventDoesNotRollBackNewerState() {
        // First: process T+10 activated event
        ObjectNode data = baseData();
        addBillingPeriod(data, PERIOD_END);
        handler.handle(payload("evt_stale_1", "subscription.activated", T_PLUS_10, data));

        CompanySubscription afterFirst = reloadSubscription();
        assertThat(afterFirst.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(afterFirst.getLastEventOccurredAt())
                .isEqualTo(LocalDateTime.of(2024, 1, 1, 10, 0, 0));

        // Then: replay with T+5 (older) via a different eventId but same type
        ObjectNode staleData = baseData();
        staleData.put("status", "past_due");
        handler.handle(payload("evt_stale_2", "subscription.updated", T_MINUS_5, staleData));

        // DB must still show the T+10 state
        CompanySubscription afterReplay = reloadSubscription();
        assertThat(afterReplay.getStatus())
                .as("Stale event must not overwrite newer status")
                .isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(afterReplay.getLastEventOccurredAt())
                .as("Timestamp must remain at T+10, not regress to T+5")
                .isEqualTo(LocalDateTime.of(2024, 1, 1, 10, 0, 0));
    }

    // -------------------------------------------------------------------------
    // Test 2: Idempotent replay — same eventId processed twice
    // Second call must be a no-op; only one row in paddle_webhook_events.
    // -------------------------------------------------------------------------

    @Test
    void idempotentReplay_sameEventIdProcessedTwice_onlyOneRecordInDB() {
        ObjectNode data = baseData();
        addBillingPeriod(data, PERIOD_END);
        PaddleWebhookPayload p = payload("evt_idem_1", "subscription.activated", T_PLUS_10, data);

        handler.handle(p);
        handler.handle(p); // second delivery — must be a no-op

        List<PaddleWebhookEvent> events = webhookEventRepository.findAll();
        assertThat(events)
                .as("Exactly one idempotency record should exist")
                .hasSize(1);
        assertThat(events.get(0).getEventId()).isEqualTo("evt_idem_1");

        // Subscription should be ACTIVE (first call succeeded)
        assertThat(reloadSubscription().getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    // -------------------------------------------------------------------------
    // Test 3: Full lifecycle — created → activated → past_due → cancelled
    // Assert final status = CANCELLED.
    // -------------------------------------------------------------------------

    @Test
    void fullLifecycle_createdActivatedPastDueCancelled_finalStatusIsCancelled() {
        // created: sets paddle IDs (subscription already linked, so this patches IDs)
        ObjectNode createdData = baseData();
        handler.handle(payload("evt_lc_1", "subscription.created", T_MINUS_5, createdData));

        // activated at T+10
        ObjectNode activatedData = baseData();
        addBillingPeriod(activatedData, PERIOD_END);
        handler.handle(payload("evt_lc_2", "subscription.activated", T_PLUS_10, activatedData));

        assertThat(reloadSubscription().getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);

        // past_due at T+15
        ObjectNode pastDueData = baseData();
        handler.handle(payload("evt_lc_3", "subscription.past_due", T_PLUS_15, pastDueData));

        assertThat(reloadSubscription().getStatus()).isEqualTo(SubscriptionStatus.PAST_DUE);

        // cancelled after T+15
        ObjectNode cancelledData = baseData();
        String T_PLUS_20 = "2024-01-01T20:00:00+00:00";
        handler.handle(payload("evt_lc_4", "subscription.cancelled", T_PLUS_20, cancelledData));

        CompanySubscription finalState = reloadSubscription();
        assertThat(finalState.getStatus())
                .as("Final lifecycle status must be CANCELLED")
                .isEqualTo(SubscriptionStatus.CANCELLED);
        assertThat(finalState.getLastEventOccurredAt())
                .isEqualTo(LocalDateTime.of(2024, 1, 1, 20, 0, 0));

        // Verify all four event records exist
        assertThat(webhookEventRepository.count()).isEqualTo(4);
    }

    // -------------------------------------------------------------------------
    // Test 4: subscription.updated dispatch — status + period both written to DB
    // -------------------------------------------------------------------------

    @Test
    void updatedDispatch_statusAndPeriodBothWrittenToDB() {
        // Put subscription into a known state first (fresh event required to pass staleness)
        // lastEventOccurredAt is null on a new subscription, so any event passes the WHERE clause.
        ObjectNode data = baseData();
        data.put("status", "active");
        addBillingPeriod(data, PERIOD_END);

        handler.handle(payload("evt_upd_1", "subscription.updated", T_PLUS_10, data));

        CompanySubscription updated = reloadSubscription();
        assertThat(updated.getStatus())
                .as("Status must be updated to ACTIVE")
                .isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(updated.getCurrentPeriodEnd())
                .as("currentPeriodEnd must be set to 2024-02-01")
                .isEqualTo(LocalDateTime.of(2024, 2, 1, 0, 0, 0));
        assertThat(updated.getLastEventOccurredAt())
                .isEqualTo(LocalDateTime.of(2024, 1, 1, 10, 0, 0));
    }

    // -------------------------------------------------------------------------
    // Test 5: subscription.resumed with period — does NOT null out existing periodEnd
    // -------------------------------------------------------------------------

    @Test
    void resumed_withoutPeriodInPayload_existingPeriodEndIsPreserved() {
        // First: activate the subscription so currentPeriodEnd is set
        ObjectNode activateData = baseData();
        addBillingPeriod(activateData, PERIOD_END);
        handler.handle(payload("evt_res_1", "subscription.activated", T_PLUS_10, activateData));

        LocalDateTime savedPeriodEnd = reloadSubscription().getCurrentPeriodEnd();
        assertThat(savedPeriodEnd).isNotNull();

        // Pause it
        handler.handle(payload("evt_res_2", "subscription.paused", T_PLUS_15, baseData()));

        // Resume without a billing period in the payload
        ObjectNode resumeData = baseData();
        // No current_billing_period node
        handler.handle(payload("evt_res_3", "subscription.resumed", "2024-01-01T20:00:00+00:00", resumeData));

        CompanySubscription afterResume = reloadSubscription();
        assertThat(afterResume.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(afterResume.getCurrentPeriodEnd())
                .as("currentPeriodEnd must be preserved — updateStatus does not touch it")
                .isEqualTo(savedPeriodEnd);
    }
}
