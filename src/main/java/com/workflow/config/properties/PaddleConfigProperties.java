package com.workflow.config.properties;

import com.workflow.common.constant.PlanType;
import com.workflow.common.exception.business.InvalidRequestException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.util.Optional;

@Configuration
@ConfigurationProperties(prefix = "paddle")
@Data
@Validated
public class PaddleConfigProperties {

    @NotBlank
    private String apiKey;

    @NotBlank
    private String webhookSecret;

    // FREE tier never checks out via Paddle, so it has no price IDs — only STARTER/PROFESSIONAL do.
    @NotBlank
    private String starterPriceId;

    @NotBlank
    private String professionalPriceId;

    @NotBlank
    private String starterExtraSeatPriceId;

    @NotBlank
    private String professionalExtraSeatPriceId;

    @NotBlank
    private String starterStorageBlockPriceId;

    @NotBlank
    private String professionalStorageBlockPriceId;

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

    public enum LineItemType {
        BASE_PLAN, EXTRA_SEAT, EXTRA_STORAGE_BLOCK
    }

    public record PriceResolution(PlanType planType, LineItemType lineItemType) {}

    /**
     * Reverse lookup used by the webhook handler: given a Paddle price ID from a subscription's
     * items[], resolve which plan tier and which kind of line item (base plan vs. an add-on) it is.
     * Returns empty for an unrecognized price ID (e.g. a stale/removed price from Paddle's dashboard).
     */
    public Optional<PriceResolution> resolvePriceId(String priceId) {
        if (priceId == null || priceId.isBlank()) {
            return Optional.empty();
        }
        if (priceId.equals(starterPriceId)) {
            return Optional.of(new PriceResolution(PlanType.STARTER, LineItemType.BASE_PLAN));
        }
        if (priceId.equals(professionalPriceId)) {
            return Optional.of(new PriceResolution(PlanType.PROFESSIONAL, LineItemType.BASE_PLAN));
        }
        if (priceId.equals(starterExtraSeatPriceId)) {
            return Optional.of(new PriceResolution(PlanType.STARTER, LineItemType.EXTRA_SEAT));
        }
        if (priceId.equals(professionalExtraSeatPriceId)) {
            return Optional.of(new PriceResolution(PlanType.PROFESSIONAL, LineItemType.EXTRA_SEAT));
        }
        if (priceId.equals(starterStorageBlockPriceId)) {
            return Optional.of(new PriceResolution(PlanType.STARTER, LineItemType.EXTRA_STORAGE_BLOCK));
        }
        if (priceId.equals(professionalStorageBlockPriceId)) {
            return Optional.of(new PriceResolution(PlanType.PROFESSIONAL, LineItemType.EXTRA_STORAGE_BLOCK));
        }
        return Optional.empty();
    }

    public String basePriceIdFor(PlanType planType) {
        return switch (planType) {
            case STARTER -> starterPriceId;
            case PROFESSIONAL -> professionalPriceId;
            case FREE -> throw new InvalidRequestException("FREE tier has no Paddle checkout price");
        };
    }

    public String extraSeatPriceIdFor(PlanType planType) {
        return switch (planType) {
            case STARTER -> starterExtraSeatPriceId;
            case PROFESSIONAL -> professionalExtraSeatPriceId;
            case FREE -> throw new InvalidRequestException("FREE tier has no extra-seat Paddle price");
        };
    }

    public String storageBlockPriceIdFor(PlanType planType) {
        return switch (planType) {
            case STARTER -> starterStorageBlockPriceId;
            case PROFESSIONAL -> professionalStorageBlockPriceId;
            case FREE -> throw new InvalidRequestException("FREE tier has no storage-block Paddle price");
        };
    }
}
