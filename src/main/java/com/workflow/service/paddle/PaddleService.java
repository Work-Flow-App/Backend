package com.workflow.service.paddle;

import com.workflow.common.constant.PlanType;
import com.workflow.config.properties.PaddleConfigProperties;
import com.workflow.dto.paddle.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class PaddleService implements IPaddleService {

    private final RestClient restClient;
    private final PaddleConfigProperties paddleProps;

    public PaddleService(
            @Qualifier("paddleRestClient") RestClient restClient,
            PaddleConfigProperties paddleProps) {
        this.restClient = restClient;
        this.paddleProps = paddleProps;
    }

    private static final Pattern EXISTING_CUSTOMER_ID_PATTERN =
            Pattern.compile("customer of id (ctm_[a-z0-9]+)");

    // Bills the prorated difference immediately on an addon-update call rather than waiting for the
    // next billing cycle or deferring the charge — matches the "purchase now" UX this endpoint is for.
    // Not currently exposed as a config option; hardcode until a caller actually needs to vary it.
    private static final String PRORATION_BILLING_MODE = "prorated_immediately";

    @Override
    public PaddleCustomerResponse createCustomer(String email, String name) {
        log.debug("Creating Paddle customer for email={}", email);
        try {
            return restClient.post()
                    .uri("/customers")
                    .body(new PaddleCustomerRequest(email, name))
                    .retrieve()
                    .body(PaddleCustomerResponse.class);
        } catch (HttpClientErrorException.Conflict e) {
            // Paddle returns 409 when customer email already exists.
            // Error detail contains the existing customer ID — extract and reuse it.
            String body = e.getResponseBodyAsString();
            Matcher matcher = EXISTING_CUSTOMER_ID_PATTERN.matcher(body);
            if (matcher.find()) {
                String existingId = matcher.group(1);
                log.info("Paddle customer already exists for email={}, reusing id={}", email, existingId);
                return new PaddleCustomerResponse(
                        new PaddleCustomerResponse.CustomerData(existingId, email, name));
            }
            throw e;
        }
    }

    @Override
    public CheckoutSessionResponse generateCheckoutUrl(
            String paddleCustomerId, Long companyId, PlanType planType, int extraSeats, int extraStorageBlocks) {
        log.debug("Generating checkout URL for paddleCustomerId={}, companyId={}, planType={}, extraSeats={}, extraStorageBlocks={}",
                paddleCustomerId, companyId, planType, extraSeats, extraStorageBlocks);

        List<GenerateCheckoutLinkRequest.CheckoutItem> items = buildLineItems(planType, extraSeats, extraStorageBlocks);

        GenerateCheckoutLinkRequest request = new GenerateCheckoutLinkRequest(
                items,
                paddleCustomerId,
                Map.of("companyId", String.valueOf(companyId)),
                paddleProps.getSuccessUrl(),
                paddleProps.getCancelUrl()
        );
        CheckoutSessionResponse session = restClient.post()
                .uri("/transactions")
                .body(request)
                .retrieve()
                .body(CheckoutSessionResponse.class);
        log.debug("Paddle transaction response: {}", session);
        return session;
    }

    /**
     * Builds the full desired line-item set for a plan tier + addon quantities. Shared by
     * generateCheckoutUrl (new transaction) and updateSubscriptionAddons (PATCH on an existing
     * subscription) — both need the identical base-plan-always-present, addon-only-if-positive shape,
     * and for the update call in particular this MUST be the complete list since Paddle's subscription
     * update replaces the entire items[] rather than merging deltas into it.
     */
    private List<GenerateCheckoutLinkRequest.CheckoutItem> buildLineItems(
            PlanType planType, int extraSeats, int extraStorageBlocks) {
        List<GenerateCheckoutLinkRequest.CheckoutItem> items = new ArrayList<>();
        items.add(new GenerateCheckoutLinkRequest.CheckoutItem(paddleProps.basePriceIdFor(planType), 1));
        if (extraSeats > 0) {
            items.add(new GenerateCheckoutLinkRequest.CheckoutItem(
                    paddleProps.extraSeatPriceIdFor(planType), extraSeats));
        }
        if (extraStorageBlocks > 0) {
            items.add(new GenerateCheckoutLinkRequest.CheckoutItem(
                    paddleProps.storageBlockPriceIdFor(planType), extraStorageBlocks));
        }
        return items;
    }

    @Override
    public PaddleSubscriptionResponse updateSubscriptionAddons(
            String paddleSubscriptionId, PlanType planType, int extraSeats, int extraStorageBlocks) {
        log.info("Updating Paddle subscription addons id={}, planType={}, extraSeats={}, extraStorageBlocks={}",
                paddleSubscriptionId, planType, extraSeats, extraStorageBlocks);

        List<GenerateCheckoutLinkRequest.CheckoutItem> items = buildLineItems(planType, extraSeats, extraStorageBlocks);
        PaddleSubscriptionUpdateRequest request =
                new PaddleSubscriptionUpdateRequest(items, PRORATION_BILLING_MODE);

        PaddleSubscriptionResponse response = restClient.patch()
                .uri("/subscriptions/{id}", paddleSubscriptionId)
                .body(request)
                .retrieve()
                .body(PaddleSubscriptionResponse.class);
        log.debug("Paddle subscription update response: {}", response);
        return response;
    }

    @Override
    public PaddleSubscriptionResponse getSubscription(String subscriptionId) {
        log.debug("Fetching Paddle subscription id={}", subscriptionId);
        return restClient.get()
                .uri("/subscriptions/{id}", subscriptionId)
                .retrieve()
                .body(PaddleSubscriptionResponse.class);
    }

    @Override
    public void cancelSubscription(String subscriptionId) {
        log.info("Cancelling Paddle subscription id={}", subscriptionId);
        restClient.post()
                .uri("/subscriptions/{id}/cancel", subscriptionId)
                .body(Map.of("effective_from", "next_billing_period"))
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public PaddlePortalSessionResponse generatePortalUrl(String paddleCustomerId) {
        log.debug("Generating portal session for paddleCustomerId={}", paddleCustomerId);
        return restClient.post()
                .uri("/customers/{id}/portal-sessions", paddleCustomerId)
                .body(Map.of())
                .retrieve()
                .body(PaddlePortalSessionResponse.class);
    }
}
