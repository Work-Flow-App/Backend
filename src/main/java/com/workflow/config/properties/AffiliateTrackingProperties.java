package com.workflow.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "affiliate.tracking")
public record AffiliateTrackingProperties(
        boolean enabled,
        String apiKey,
        String accountId,
        String baseUrl,
        String signupEndpoint,
        int connectTimeoutSeconds,
        int readTimeoutSeconds
) {}
