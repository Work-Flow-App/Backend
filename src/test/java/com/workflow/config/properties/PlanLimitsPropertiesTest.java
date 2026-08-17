package com.workflow.config.properties;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PlanLimitsPropertiesTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private static PlanLimitsProperties.Tier tier(int maxUsers, String extraUserPrice, int jobsPerMonth,
            long storageLimitBytes, long storageOverageBlockBytes, String storageOveragePrice, String monthlyPrice) {
        PlanLimitsProperties.Tier tier = new PlanLimitsProperties.Tier();
        tier.setMaxUsers(maxUsers);
        tier.setExtraUserPrice(new BigDecimal(extraUserPrice));
        tier.setJobsPerMonth(jobsPerMonth);
        tier.setStorageLimitBytes(storageLimitBytes);
        tier.setStorageOverageBlockBytes(storageOverageBlockBytes);
        tier.setStorageOveragePrice(new BigDecimal(storageOveragePrice));
        tier.setMonthlyPrice(new BigDecimal(monthlyPrice));
        return tier;
    }

    // Mirrors the values wired into application.yml
    private static PlanLimitsProperties validConfig() {
        PlanLimitsProperties props = new PlanLimitsProperties();
        props.setFree(tier(1, "0", 10, 250_000_000L, 0L, "0", "0"));
        props.setStarter(tier(3, "10", 150, 10_000_000_000L, 3_000_000_000L, "10", "39"));
        props.setProfessional(tier(8, "8", 200, 30_000_000_000L, 5_000_000_000L, "10", "79"));
        return props;
    }

    @Test
    void validConfig_HasNoViolations() {
        Set<ConstraintViolation<PlanLimitsProperties>> violations = validator.validate(validConfig());

        assertThat(violations).isEmpty();
    }

    @Test
    void starterMonthlyPriceZero_FailsPaidTierPricingAssertion() {
        PlanLimitsProperties props = validConfig();
        props.getStarter().setMonthlyPrice(BigDecimal.ZERO);

        Set<ConstraintViolation<PlanLimitsProperties>> violations = validator.validate(props);

        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("paidTierPricingValid"));
    }

    @Test
    void professionalMonthlyPriceZero_FailsPaidTierPricingAssertion() {
        PlanLimitsProperties props = validConfig();
        props.getProfessional().setMonthlyPrice(BigDecimal.ZERO);

        Set<ConstraintViolation<PlanLimitsProperties>> violations = validator.validate(props);

        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("paidTierPricingValid"));
    }

    @Test
    void starterMonthlyPriceNegative_FailsFieldLevelConstraintToo() {
        PlanLimitsProperties props = validConfig();
        props.getStarter().setMonthlyPrice(new BigDecimal("-1"));

        Set<ConstraintViolation<PlanLimitsProperties>> violations = validator.validate(props);

        // Negative fails Tier.monthlyPrice's own @PositiveOrZero, independent of the paid-tier assertion
        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("starter.monthlyPrice"));
    }

    @Test
    void freeMonthlyPriceZero_IsValid() {
        PlanLimitsProperties props = validConfig(); // free.monthlyPrice is already 0

        Set<ConstraintViolation<PlanLimitsProperties>> violations = validator.validate(props);

        assertThat(violations).isEmpty();
    }
}
