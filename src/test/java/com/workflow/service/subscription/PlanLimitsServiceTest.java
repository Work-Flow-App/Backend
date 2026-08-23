package com.workflow.service.subscription;

import com.workflow.common.constant.PlanType;
import com.workflow.common.exception.business.InvalidRequestException;
import com.workflow.config.properties.PlanLimitsProperties;
import com.workflow.entity.company.CompanySubscription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlanLimitsServiceTest {

    private PlanLimitsService planLimitsService;

    @BeforeEach
    void setUp() {
        PlanLimitsProperties planLimitsProperties = new PlanLimitsProperties();
        planLimitsProperties.setFree(tier(1, "0", 10, 250_000_000L, 0L, "0", "0"));
        planLimitsProperties.setStarter(tier(3, "10", 150, 10_000_000_000L, 3_000_000_000L, "10", "39"));
        planLimitsProperties.setProfessional(tier(8, "8", 200, 30_000_000_000L, 5_000_000_000L, "10", "79"));

        planLimitsService = new PlanLimitsService(planLimitsProperties);
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

    private static CompanySubscription subscription(PlanType planType, int extraUserSeats, int extraStorageBlocks) {
        return CompanySubscription.builder()
                .planType(planType)
                .extraUserSeats(extraUserSeats)
                .extraStorageBlocks(extraStorageBlocks)
                .build();
    }

    // ============= getEffectiveMaxUsers =============

    @Test
    void getEffectiveMaxUsers_Free_AddsExtraSeatsToBase() {
        assertThat(planLimitsService.getEffectiveMaxUsers(subscription(PlanType.FREE, 2, 0))).isEqualTo(3);
    }

    @Test
    void getEffectiveMaxUsers_Starter_AddsExtraSeatsToBase() {
        assertThat(planLimitsService.getEffectiveMaxUsers(subscription(PlanType.STARTER, 5, 0))).isEqualTo(8);
    }

    @Test
    void getEffectiveMaxUsers_Professional_AddsExtraSeatsToBase() {
        assertThat(planLimitsService.getEffectiveMaxUsers(subscription(PlanType.PROFESSIONAL, 1, 0))).isEqualTo(9);
    }

    @Test
    void getEffectiveMaxUsers_NoExtraSeats_ReturnsBaseOnly() {
        assertThat(planLimitsService.getEffectiveMaxUsers(subscription(PlanType.STARTER, 0, 0))).isEqualTo(3);
    }

    // ============= getEffectiveJobsPerMonth =============

    @Test
    void getEffectiveJobsPerMonth_Free_ReturnsTierBase() {
        assertThat(planLimitsService.getEffectiveJobsPerMonth(subscription(PlanType.FREE, 0, 0))).isEqualTo(10);
    }

    @Test
    void getEffectiveJobsPerMonth_Starter_ReturnsTierBase() {
        assertThat(planLimitsService.getEffectiveJobsPerMonth(subscription(PlanType.STARTER, 0, 0))).isEqualTo(150);
    }

    @Test
    void getEffectiveJobsPerMonth_Professional_ReturnsTierBase() {
        assertThat(planLimitsService.getEffectiveJobsPerMonth(subscription(PlanType.PROFESSIONAL, 0, 0))).isEqualTo(200);
    }

    // ============= getEffectiveStorageLimitBytes =============

    @Test
    void getEffectiveStorageLimitBytes_Free_NoOverageBlocksPurchased_ReturnsBaseOnly() {
        assertThat(planLimitsService.getEffectiveStorageLimitBytes(subscription(PlanType.FREE, 0, 0)))
                .isEqualTo(250_000_000L);
    }

    @Test
    void getEffectiveStorageLimitBytes_Starter_AddsPurchasedOverageBlocks() {
        long expected = 10_000_000_000L + 2 * 3_000_000_000L;
        assertThat(planLimitsService.getEffectiveStorageLimitBytes(subscription(PlanType.STARTER, 0, 2)))
                .isEqualTo(expected);
    }

    @Test
    void getEffectiveStorageLimitBytes_Professional_AddsPurchasedOverageBlocks() {
        long expected = 30_000_000_000L + 3 * 5_000_000_000L;
        assertThat(planLimitsService.getEffectiveStorageLimitBytes(subscription(PlanType.PROFESSIONAL, 0, 3)))
                .isEqualTo(expected);
    }

    // ============= null subscription guard (direct CompanySubscription overload) =============
    // Cast disambiguates which overload is meant — a bare `null` is ambiguous now that an
    // Optional<CompanySubscription>-accepting overload also exists (see below).

    @Test
    void getEffectiveMaxUsers_NullSubscription_ThrowsInvalidRequestException() {
        assertThatThrownBy(() -> planLimitsService.getEffectiveMaxUsers((CompanySubscription) null))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void getEffectiveJobsPerMonth_NullSubscription_ThrowsInvalidRequestException() {
        assertThatThrownBy(() -> planLimitsService.getEffectiveJobsPerMonth((CompanySubscription) null))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void getEffectiveStorageLimitBytes_NullSubscription_ThrowsInvalidRequestException() {
        assertThatThrownBy(() -> planLimitsService.getEffectiveStorageLimitBytes((CompanySubscription) null))
                .isInstanceOf(InvalidRequestException.class);
    }

    // ============= Optional<CompanySubscription> overloads — fail-CLOSED to FREE tier =============

    @Test
    void getEffectiveMaxUsers_EmptyOptional_DefaultsToFreeTier() {
        assertThat(planLimitsService.getEffectiveMaxUsers(Optional.<CompanySubscription>empty())).isEqualTo(1);
    }

    @Test
    void getEffectiveJobsPerMonth_EmptyOptional_DefaultsToFreeTier() {
        assertThat(planLimitsService.getEffectiveJobsPerMonth(Optional.<CompanySubscription>empty())).isEqualTo(10);
    }

    @Test
    void getEffectiveStorageLimitBytes_EmptyOptional_DefaultsToFreeTier() {
        assertThat(planLimitsService.getEffectiveStorageLimitBytes(Optional.<CompanySubscription>empty()))
                .isEqualTo(250_000_000L);
    }

    @Test
    void getEffectiveMaxUsers_PresentOptional_DelegatesToRealSubscription() {
        CompanySubscription starter = subscription(PlanType.STARTER, 2, 0);
        assertThat(planLimitsService.getEffectiveMaxUsers(Optional.of(starter))).isEqualTo(5); // 3 base + 2 extra
    }
}
