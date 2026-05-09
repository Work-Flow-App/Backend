package com.workflow.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "paddle")
@Data
@Validated
public class PaddleConfigProperties {

    @NotBlank
    private String apiKey;

    @NotBlank
    private String webhookSecret;

    @NotBlank
    private String priceId;

    @NotBlank
    private String apiBaseUrl;

    @NotBlank
    private String successUrl;

    @NotBlank
    private String cancelUrl;

    @Positive
    private int trialDays = 14;

    @Positive
    private int pastDueGraceDays = 3;

    @Positive
    private int webhookTimestampToleranceSeconds = 300;
}