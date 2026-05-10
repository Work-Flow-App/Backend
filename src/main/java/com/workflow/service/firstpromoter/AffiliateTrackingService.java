package com.workflow.service.firstpromoter;

import com.workflow.config.properties.AffiliateTrackingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AffiliateTrackingService {

    private static final Pattern VALID_TID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,64}$");

    private final AffiliateTrackingProperties properties;
    @Qualifier("affiliateTrackingRestClient")
    private final RestClient affiliateTrackingRestClient;

    @Async("affiliateTrackingExecutor")
    public void trackSignup(String email, String tid) {
        if (!properties.enabled() || properties.apiKey() == null || properties.apiKey().isBlank()) {
            log.debug("Affiliate tracking disabled or API key not configured — skipping signup tracking");
            return;
        }

        String sanitizedTid = sanitizeTid(tid);

        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        if (sanitizedTid != null) {
            body.put("tid", sanitizedTid);
        }

        try {
            affiliateTrackingRestClient.post()
                    .uri(properties.signupEndpoint())
                    .header("Authorization", "Bearer " + properties.apiKey())
                    .header("Account-ID", properties.accountId())
                    .header("Content-Type", "application/json")
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Affiliate signup tracked for user: {}", email);
        } catch (Exception e) {
            log.warn("Affiliate signup tracking failed for user {}: {}", email, e.getMessage());
        }
    }

    private String sanitizeTid(String tid) {
        if (tid == null || tid.isBlank()) {
            return null;
        }
        if (!VALID_TID_PATTERN.matcher(tid).matches()) {
            log.warn("Affiliate tracking: received invalid tid — ignoring. length={}", tid.length());
            return null;
        }
        return tid;
    }
}
