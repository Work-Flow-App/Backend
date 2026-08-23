package com.workflow.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

@Configuration
@ConfigurationProperties(prefix = "plan-limits")
@Data
@Validated
public class PlanLimitsProperties {

    @NotNull
    @Valid
    private Tier free;

    @NotNull
    @Valid
    private Tier starter;

    @NotNull
    @Valid
    private Tier professional;

    // @PositiveOrZero on Tier.monthlyPrice allows FREE's legitimate $0 — this catches the
    // narrower case of a paid tier misconfigured to $0, which would silently zero out revenue.
    @AssertTrue(message = "starter and professional monthlyPrice must be greater than zero")
    public boolean isPaidTierPricingValid() {
        if (starter == null || professional == null) {
            return true; // let @NotNull report the real problem instead of piling on here
        }
        return starter.getMonthlyPrice() != null && starter.getMonthlyPrice().signum() > 0
                && professional.getMonthlyPrice() != null && professional.getMonthlyPrice().signum() > 0;
    }

    @Data
    public static class Tier {

        @Positive
        private int maxUsers;

        @PositiveOrZero
        private BigDecimal extraUserPrice;

        @Positive
        private int jobsPerMonth;

        @Positive
        private long storageLimitBytes;

        @PositiveOrZero
        private long storageOverageBlockBytes;

        @PositiveOrZero
        private BigDecimal storageOveragePrice;

        @PositiveOrZero
        private BigDecimal monthlyPrice;
    }
}
