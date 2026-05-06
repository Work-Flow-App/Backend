package com.workflow.service.paddle;

import com.workflow.config.properties.PaddleConfigProperties;
import com.workflow.dto.paddle.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

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
    public CheckoutSessionResponse generateCheckoutUrl(String paddleCustomerId, Long companyId) {
        log.debug("Generating checkout URL for paddleCustomerId={}, companyId={}", paddleCustomerId, companyId);
        GenerateCheckoutLinkRequest request = new GenerateCheckoutLinkRequest(
                List.of(new GenerateCheckoutLinkRequest.CheckoutItem(paddleProps.getPriceId(), 1)),
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
