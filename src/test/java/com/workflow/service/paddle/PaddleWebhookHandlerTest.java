package com.workflow.service.paddle;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.common.constant.PlanType;
import com.workflow.common.constant.SubscriptionStatus;
import com.workflow.config.properties.PaddleConfigProperties;
import com.workflow.dto.paddle.PaddleWebhookPayload;
import com.workflow.entity.company.Company;
import com.workflow.entity.company.CompanySubscription;
import com.workflow.entity.company.PaddleWebhookEvent;
import com.workflow.repository.company.CompanySubscriptionRepository;
import com.workflow.repository.company.PaddleWebhookEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaddleWebhookHandlerTest {

    @Mock
    private CompanySubscriptionRepository subscriptionRepository;

    @Mock
    private PaddleWebhookEventRepository webhookEventRepository;

    @Mock
    private PaddleConfigProperties paddleProps;

    @InjectMocks
    private PaddleWebhookHandler handler;

    private final ObjectMapper om = new ObjectMapper();

    // T+10: the "newer" timestamp used for the primary event
    private static final String OCCURRED_AT_T10 = "2024-01-01T10:00:00+00:00";
    // T+5: an older timestamp used to simulate a stale/out-of-order replay
    private static final String OCCURRED_AT_T5  = "2024-01-01T05:00:00+00:00";
    private static final String PERIOD_END_ISO  = "2024-02-01T00:00:00+00:00";

    private CompanySubscription subscription;

    @BeforeEach
    void setUp() {
        Company company = Company.builder().id(1L).name("Acme").build();
        subscription = CompanySubscription.builder()
                .id(42L)
                .company(company)
                .trialEndsAt(LocalDateTime.now(ZoneOffset.UTC).plusDays(7))
                .status(SubscriptionStatus.TRIAL)
                .paddleSubscriptionId("sub_123")
                .build();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private ObjectNode baseData(String paddleSubId) {
        ObjectNode data = om.createObjectNode();
        data.put("id", paddleSubId);
        data.put("customer_id", "cust_999");
        ObjectNode customData = om.createObjectNode();
        customData.put("companyId", "1");
        data.set("custom_data", customData);
        return data;
    }

    private void addBillingPeriod(ObjectNode data, String endsAt) {
        ObjectNode period = om.createObjectNode();
        period.put("ends_at", endsAt);
        data.set("current_billing_period", period);
    }

    private ObjectNode item(String priceId, int quantity) {
        ObjectNode item = om.createObjectNode();
        ObjectNode price = om.createObjectNode();
        price.put("id", priceId);
        item.set("price", price);
        item.put("quantity", quantity);
        return item;
    }

    private void addItems(ObjectNode data, ObjectNode... items) {
        ArrayNode arr = om.createArrayNode();
        for (ObjectNode i : items) {
            arr.add(i);
        }
        data.set("items", arr);
    }

    private PaddleWebhookPayload payload(String eventId, String eventType, String occurredAt, ObjectNode data) {
        return new PaddleWebhookPayload(null, eventId, eventType, occurredAt, data);
    }

    private void givenEventIsNew() {
        when(webhookEventRepository.existsById(any())).thenReturn(false);
        when(webhookEventRepository.save(any(PaddleWebhookEvent.class))).thenAnswer(i -> i.getArgument(0));
    }

    private void givenEventAlreadySeen() {
        when(webhookEventRepository.existsById(any())).thenReturn(true);
    }

    private void givenSubscriptionFound() {
        when(subscriptionRepository.findByCompanyId(1L)).thenReturn(Optional.of(subscription));
    }

    private void givenSubscriptionNotFound() {
        when(subscriptionRepository.findByCompanyId(1L)).thenReturn(Optional.empty());
        when(subscriptionRepository.findByPaddleSubscriptionId(anyString())).thenReturn(Optional.empty());
        when(subscriptionRepository.findByPaddleCustomerId(anyString())).thenReturn(Optional.empty());
    }

    // -------------------------------------------------------------------------
    // subscription.activated — fresh event
    // -------------------------------------------------------------------------

    @Test
    void activated_freshEvent_callsUpdateActivatedWithCorrectArgs() {
        givenEventIsNew();
        givenSubscriptionFound();
        when(subscriptionRepository.updateActivated(
                eq(42L), eq(SubscriptionStatus.ACTIVE), any(), any(), any(), any(), any()))
                .thenReturn(1);

        ObjectNode data = baseData("sub_123");
        addBillingPeriod(data, PERIOD_END_ISO);

        handler.handle(payload("evt_1", "subscription.activated", OCCURRED_AT_T10, data));

        ArgumentCaptor<LocalDateTime> periodCaptor  = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> occurredCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(subscriptionRepository).updateActivated(
                eq(42L), eq(SubscriptionStatus.ACTIVE), periodCaptor.capture(),
                isNull(), isNull(), isNull(), occurredCaptor.capture());

        // 10:00 UTC → LocalDateTime
        assertThat(occurredCaptor.getValue()).isEqualTo(LocalDateTime.of(2024, 1, 1, 10, 0, 0));
        assertThat(periodCaptor.getValue()).isEqualTo(LocalDateTime.of(2024, 2, 1, 0, 0, 0));
    }

    // -------------------------------------------------------------------------
    // subscription.activated — stale event (DB returns 0 rows)
    // -------------------------------------------------------------------------

    @Test
    void activated_staleEvent_noException_updateActivatedCalledOnce() {
        givenEventIsNew();
        givenSubscriptionFound();
        when(subscriptionRepository.updateActivated(any(), any(), any(), any(), any(), any(), any())).thenReturn(0);

        ObjectNode data = baseData("sub_123");
        addBillingPeriod(data, PERIOD_END_ISO);

        handler.handle(payload("evt_2", "subscription.activated", OCCURRED_AT_T5, data));

        // Should call updateActivated exactly once, no exception thrown, no additional writes
        verify(subscriptionRepository, times(1)).updateActivated(any(), any(), any(), any(), any(), any(), any());
        verify(subscriptionRepository, never()).save(any(CompanySubscription.class));
    }

    // -------------------------------------------------------------------------
    // subscription.activated — subscription not found
    // -------------------------------------------------------------------------

    @Test
    void activated_subscriptionNotFound_noUpdateCalled() {
        givenEventIsNew();
        givenSubscriptionNotFound();

        ObjectNode data = baseData("sub_123");
        handler.handle(payload("evt_3", "subscription.activated", OCCURRED_AT_T10, data));

        verify(subscriptionRepository, never()).updateActivated(any(), any(), any(), any(), any(), any(), any());
    }

    // -------------------------------------------------------------------------
    // subscription.activated — items[] resolve a full multi-tier plan snapshot
    // -------------------------------------------------------------------------

    @Test
    void activated_withItemsSnapshot_resolvesPlanSeatsAndStorage() {
        givenEventIsNew();
        givenSubscriptionFound();
        when(paddleProps.resolvePriceId("price_pro_base")).thenReturn(Optional.of(
                new PaddleConfigProperties.PriceResolution(PlanType.PROFESSIONAL,
                        PaddleConfigProperties.LineItemType.BASE_PLAN)));
        when(paddleProps.resolvePriceId("price_pro_seat")).thenReturn(Optional.of(
                new PaddleConfigProperties.PriceResolution(PlanType.PROFESSIONAL,
                        PaddleConfigProperties.LineItemType.EXTRA_SEAT)));
        when(paddleProps.resolvePriceId("price_pro_storage")).thenReturn(Optional.of(
                new PaddleConfigProperties.PriceResolution(PlanType.PROFESSIONAL,
                        PaddleConfigProperties.LineItemType.EXTRA_STORAGE_BLOCK)));
        when(subscriptionRepository.updateActivated(
                eq(42L), eq(SubscriptionStatus.ACTIVE), any(), any(), any(), any(), any()))
                .thenReturn(1);

        ObjectNode data = baseData("sub_123");
        addBillingPeriod(data, PERIOD_END_ISO);
        addItems(data,
                item("price_pro_base", 1),
                item("price_pro_seat", 2),
                item("price_pro_storage", 1));

        handler.handle(payload("evt_items_1", "subscription.activated", OCCURRED_AT_T10, data));

        verify(subscriptionRepository).updateActivated(
                eq(42L), eq(SubscriptionStatus.ACTIVE), any(),
                eq(PlanType.PROFESSIONAL), eq(2), eq(1), any());
    }

    // -------------------------------------------------------------------------
    // subscription.activated — unresolvable price ID is skipped, not thrown
    // -------------------------------------------------------------------------

    @Test
    void activated_withUnresolvablePriceId_skipsPlanUpdateWithoutThrowing() {
        givenEventIsNew();
        givenSubscriptionFound();
        when(paddleProps.resolvePriceId("price_unknown")).thenReturn(Optional.empty());
        when(subscriptionRepository.updateActivated(
                eq(42L), eq(SubscriptionStatus.ACTIVE), any(), any(), any(), any(), any()))
                .thenReturn(1);

        ObjectNode data = baseData("sub_123");
        addBillingPeriod(data, PERIOD_END_ISO);
        addItems(data, item("price_unknown", 1));

        assertThatCode(() ->
                handler.handle(payload("evt_items_2", "subscription.activated", OCCURRED_AT_T10, data)))
                .doesNotThrowAnyException();

        // No resolvable BASE_PLAN item -> plan/seat/storage params must be null (untouched), not garbage
        verify(subscriptionRepository).updateActivated(
                eq(42L), eq(SubscriptionStatus.ACTIVE), any(),
                isNull(), isNull(), isNull(), any());
    }

    // -------------------------------------------------------------------------
    // subscription.updated — status + period both present
    // -------------------------------------------------------------------------

    @Test
    void updated_statusAndPeriodPresent_callsUpdateStatusAndPeriod() {
        givenEventIsNew();
        givenSubscriptionFound();
        when(subscriptionRepository.updateStatusAndPeriod(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1);

        ObjectNode data = baseData("sub_123");
        data.put("status", "active");
        addBillingPeriod(data, PERIOD_END_ISO);

        handler.handle(payload("evt_4", "subscription.updated", OCCURRED_AT_T10, data));

        verify(subscriptionRepository).updateStatusAndPeriod(
                eq(42L), eq(SubscriptionStatus.ACTIVE), any(LocalDateTime.class),
                isNull(), isNull(), isNull(), any(LocalDateTime.class));
        verify(subscriptionRepository, never()).updateStatusOnly(any(), any(), any(), any(), any(), any());
        verify(subscriptionRepository, never()).updatePeriodOnly(any(), any(), any(), any(), any(), any());
        verify(subscriptionRepository, never()).updateTimestampOnly(any(), any(), any(), any(), any());
    }

    // -------------------------------------------------------------------------
    // subscription.updated — status only (no billing period)
    // -------------------------------------------------------------------------

    @Test
    void updated_statusOnlyPresent_callsUpdateStatusOnly() {
        givenEventIsNew();
        givenSubscriptionFound();
        when(subscriptionRepository.updateStatusOnly(any(), any(), any(), any(), any(), any())).thenReturn(1);

        ObjectNode data = baseData("sub_123");
        data.put("status", "past_due");
        // No current_billing_period node

        handler.handle(payload("evt_5", "subscription.updated", OCCURRED_AT_T10, data));

        verify(subscriptionRepository).updateStatusOnly(
                eq(42L), eq(SubscriptionStatus.PAST_DUE), isNull(), isNull(), isNull(), any(LocalDateTime.class));
        verify(subscriptionRepository, never()).updateStatusAndPeriod(any(), any(), any(), any(), any(), any(), any());
        verify(subscriptionRepository, never()).updatePeriodOnly(any(), any(), any(), any(), any(), any());
        verify(subscriptionRepository, never()).updateTimestampOnly(any(), any(), any(), any(), any());
    }

    // -------------------------------------------------------------------------
    // subscription.updated — period only (unknown Paddle status)
    // -------------------------------------------------------------------------

    @Test
    void updated_periodOnlyPresent_unknownStatus_callsUpdatePeriodOnly() {
        givenEventIsNew();
        givenSubscriptionFound();
        when(subscriptionRepository.updatePeriodOnly(any(), any(), any(), any(), any(), any())).thenReturn(1);

        ObjectNode data = baseData("sub_123");
        data.put("status", "trialing"); // not in our mapPaddleStatus switch
        addBillingPeriod(data, PERIOD_END_ISO);

        handler.handle(payload("evt_6", "subscription.updated", OCCURRED_AT_T10, data));

        verify(subscriptionRepository).updatePeriodOnly(
                eq(42L), any(LocalDateTime.class), isNull(), isNull(), isNull(), any(LocalDateTime.class));
        verify(subscriptionRepository, never()).updateStatusAndPeriod(any(), any(), any(), any(), any(), any(), any());
        verify(subscriptionRepository, never()).updateStatusOnly(any(), any(), any(), any(), any(), any());
        verify(subscriptionRepository, never()).updateTimestampOnly(any(), any(), any(), any(), any());
    }

    // -------------------------------------------------------------------------
    // subscription.updated — neither (unknown status + no period)
    // -------------------------------------------------------------------------

    @Test
    void updated_neitherStatusNorPeriod_callsUpdateTimestampOnly() {
        givenEventIsNew();
        givenSubscriptionFound();
        when(subscriptionRepository.updateTimestampOnly(any(), any(), any(), any(), any())).thenReturn(1);

        ObjectNode data = baseData("sub_123");
        data.put("status", "trialing"); // unmapped
        // No current_billing_period

        handler.handle(payload("evt_7", "subscription.updated", OCCURRED_AT_T10, data));

        verify(subscriptionRepository).updateTimestampOnly(
                eq(42L), isNull(), isNull(), isNull(), any(LocalDateTime.class));
        verify(subscriptionRepository, never()).updateStatusAndPeriod(any(), any(), any(), any(), any(), any(), any());
        verify(subscriptionRepository, never()).updateStatusOnly(any(), any(), any(), any(), any(), any());
        verify(subscriptionRepository, never()).updatePeriodOnly(any(), any(), any(), any(), any(), any());
    }

    // -------------------------------------------------------------------------
    // subscription.updated — stale (0 rows) — no exception
    // -------------------------------------------------------------------------

    @Test
    void updated_staleEvent_noException() {
        givenEventIsNew();
        givenSubscriptionFound();
        when(subscriptionRepository.updateStatusAndPeriod(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(0);

        ObjectNode data = baseData("sub_123");
        data.put("status", "active");
        addBillingPeriod(data, PERIOD_END_ISO);

        handler.handle(payload("evt_8", "subscription.updated", OCCURRED_AT_T5, data));

        // 0 rows — method was called, no exception
        verify(subscriptionRepository).updateStatusAndPeriod(any(), any(), any(), any(), any(), any(), any());
    }

    // -------------------------------------------------------------------------
    // subscription.cancelled — fresh
    // -------------------------------------------------------------------------

    @Test
    void cancelled_freshEvent_callsUpdateStatusWithCancelled() {
        givenEventIsNew();
        givenSubscriptionFound();
        when(subscriptionRepository.updateStatus(eq(42L), eq(SubscriptionStatus.CANCELLED), any())).thenReturn(1);

        ObjectNode data = baseData("sub_123");
        handler.handle(payload("evt_9", "subscription.cancelled", OCCURRED_AT_T10, data));

        verify(subscriptionRepository).updateStatus(eq(42L), eq(SubscriptionStatus.CANCELLED), any(LocalDateTime.class));
    }

    // -------------------------------------------------------------------------
    // subscription.cancelled — stale (0 rows) — no exception
    // -------------------------------------------------------------------------

    @Test
    void cancelled_staleEvent_noException() {
        givenEventIsNew();
        givenSubscriptionFound();
        when(subscriptionRepository.updateStatus(eq(42L), eq(SubscriptionStatus.CANCELLED), any())).thenReturn(0);

        ObjectNode data = baseData("sub_123");
        handler.handle(payload("evt_10", "subscription.cancelled", OCCURRED_AT_T5, data));

        verify(subscriptionRepository, times(1)).updateStatus(any(), any(), any());
    }

    // -------------------------------------------------------------------------
    // subscription.resumed — fresh
    // -------------------------------------------------------------------------

    @Test
    void resumed_freshEvent_setsStatusToActive() {
        givenEventIsNew();
        givenSubscriptionFound();
        when(subscriptionRepository.updateStatus(eq(42L), eq(SubscriptionStatus.ACTIVE), any())).thenReturn(1);

        ObjectNode data = baseData("sub_123");
        // No billing period — verify updatePeriodOnly is NOT called
        handler.handle(payload("evt_11", "subscription.resumed", OCCURRED_AT_T10, data));

        verify(subscriptionRepository).updateStatus(eq(42L), eq(SubscriptionStatus.ACTIVE), any(LocalDateTime.class));
        verify(subscriptionRepository, never()).updatePeriodOnly(any(), any(), any(), any(), any(), any());
    }

    @Test
    void resumed_freshEventWithPeriod_setsStatusAndUpdatesPeriod() {
        givenEventIsNew();
        givenSubscriptionFound();
        when(subscriptionRepository.updateStatus(eq(42L), eq(SubscriptionStatus.ACTIVE), any())).thenReturn(1);
        when(subscriptionRepository.updatePeriodOnly(eq(42L), any(), any(), any(), any(), any())).thenReturn(1);

        ObjectNode data = baseData("sub_123");
        addBillingPeriod(data, PERIOD_END_ISO);

        handler.handle(payload("evt_12", "subscription.resumed", OCCURRED_AT_T10, data));

        verify(subscriptionRepository).updateStatus(eq(42L), eq(SubscriptionStatus.ACTIVE), any(LocalDateTime.class));
        verify(subscriptionRepository).updatePeriodOnly(
                eq(42L), any(LocalDateTime.class), isNull(), isNull(), isNull(), any(LocalDateTime.class));
    }

    @Test
    void resumed_staleEvent_noPeriodUpdateCalled() {
        givenEventIsNew();
        givenSubscriptionFound();
        // 0 rows = stale, handler must return early without calling updatePeriodOnly
        when(subscriptionRepository.updateStatus(eq(42L), eq(SubscriptionStatus.ACTIVE), any())).thenReturn(0);

        ObjectNode data = baseData("sub_123");
        addBillingPeriod(data, PERIOD_END_ISO);

        handler.handle(payload("evt_13", "subscription.resumed", OCCURRED_AT_T5, data));

        verify(subscriptionRepository).updateStatus(any(), any(), any());
        verify(subscriptionRepository, never()).updatePeriodOnly(any(), any(), any(), any(), any(), any());
    }

    // -------------------------------------------------------------------------
    // Idempotency guard: already-seen eventId
    // -------------------------------------------------------------------------

    @Test
    void handle_duplicateEventId_noHandlerCalled() {
        givenEventAlreadySeen();

        ObjectNode data = baseData("sub_123");
        data.put("status", "active");
        addBillingPeriod(data, PERIOD_END_ISO);

        handler.handle(payload("evt_dup", "subscription.activated", OCCURRED_AT_T10, data));

        // Repository must not be touched at all for data mutations
        verify(subscriptionRepository, never()).updateActivated(any(), any(), any(), any(), any(), any(), any());
        verify(subscriptionRepository, never()).updateStatus(any(), any(), any());
        verify(subscriptionRepository, never()).updateStatusAndPeriod(any(), any(), any(), any(), any(), any(), any());
        verify(subscriptionRepository, never()).updateStatusOnly(any(), any(), any(), any(), any(), any());
        verify(subscriptionRepository, never()).updatePeriodOnly(any(), any(), any(), any(), any(), any());
        verify(subscriptionRepository, never()).updateTimestampOnly(any(), any(), any(), any(), any());
        verify(subscriptionRepository, never()).save(any());
        // No idempotency record saved either
        verify(webhookEventRepository, never()).save(any());
    }
}
