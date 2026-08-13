package com.workflow.service.subscription;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.workflow.common.constant.PlanType;
import com.workflow.common.exception.business.StorageLimitExceededException;
import com.workflow.entity.company.CompanySubscription;
import com.workflow.repository.company.CompanyRepository;
import com.workflow.repository.company.CompanySubscriptionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StorageQuotaServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CompanySubscriptionRepository subscriptionRepository;

    @Mock
    private IPlanLimitsService planLimitsService;

    @InjectMocks
    private StorageQuotaService storageQuotaService;

    private Logger logger;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(StorageQuotaService.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(logAppender);
    }

    // ============= recordUpload / recordDelete =============

    @Test
    void recordUpload_CallsIncrementWithPositiveDelta() {
        storageQuotaService.recordUpload(1L, 500L);

        verify(companyRepository).incrementStorageUsedBytes(1L, 500L);
        verifyNoMoreInteractions(companyRepository);
    }

    @Test
    void recordDelete_CallsDecrementWithPositiveDelta() {
        // decrement query itself does bytes-subtraction + floor(0) — the service just passes the magnitude through
        storageQuotaService.recordDelete(1L, 500L);

        verify(companyRepository).decrementStorageUsedBytes(1L, 500L);
        verifyNoMoreInteractions(companyRepository);
    }

    // ============= assertCapacity =============
    // Phase 3: flipped from log-only to throwing. "No subscription" now fails CLOSED to
    // FREE-tier defaults (previously failed open / skipped the check entirely in Phase 2).

    @Test
    void assertCapacity_WithinLimit_DoesNotThrow() {
        CompanySubscription subscription = CompanySubscription.builder().planType(PlanType.STARTER).build();
        when(subscriptionRepository.findByCompanyId(1L)).thenReturn(Optional.of(subscription));
        when(companyRepository.findStorageUsedBytes(1L)).thenReturn(Optional.of(1_000_000L));
        when(planLimitsService.getEffectiveStorageLimitBytes(Optional.of(subscription))).thenReturn(10_000_000_000L);

        assertThatCode(() -> storageQuotaService.assertCapacity(1L, 500_000L)).doesNotThrowAnyException();
    }

    @Test
    void assertCapacity_ExceedsLimit_ThrowsStorageLimitExceededException() {
        CompanySubscription subscription = CompanySubscription.builder().planType(PlanType.FREE).build();
        when(subscriptionRepository.findByCompanyId(1L)).thenReturn(Optional.of(subscription));
        when(companyRepository.findStorageUsedBytes(1L)).thenReturn(Optional.of(249_000_000L));
        when(planLimitsService.getEffectiveStorageLimitBytes(Optional.of(subscription))).thenReturn(250_000_000L);

        // 249M used + 5M incoming > 250M limit
        assertThatThrownBy(() -> storageQuotaService.assertCapacity(1L, 5_000_000L))
                .isInstanceOf(StorageLimitExceededException.class);
    }

    @Test
    void assertCapacity_ExactlyAtLimit_DoesNotThrow() {
        CompanySubscription subscription = CompanySubscription.builder().planType(PlanType.FREE).build();
        when(subscriptionRepository.findByCompanyId(1L)).thenReturn(Optional.of(subscription));
        when(companyRepository.findStorageUsedBytes(1L)).thenReturn(Optional.of(240_000_000L));
        when(planLimitsService.getEffectiveStorageLimitBytes(Optional.of(subscription))).thenReturn(250_000_000L);

        // 240M + 10M == 250M limit exactly — not over
        assertThatCode(() -> storageQuotaService.assertCapacity(1L, 10_000_000L)).doesNotThrowAnyException();
    }

    @Test
    void assertCapacity_NoUsageRecordedYet_TreatsCurrentUsageAsZero() {
        CompanySubscription subscription = CompanySubscription.builder().planType(PlanType.STARTER).build();
        when(subscriptionRepository.findByCompanyId(1L)).thenReturn(Optional.of(subscription));
        when(companyRepository.findStorageUsedBytes(1L)).thenReturn(Optional.empty());
        when(planLimitsService.getEffectiveStorageLimitBytes(Optional.of(subscription))).thenReturn(10_000_000_000L);

        assertThatCode(() -> storageQuotaService.assertCapacity(1L, 500L)).doesNotThrowAnyException();
    }

    // Decision: fail CLOSED to FREE-tier limits (not open/unlimited) when no CompanySubscription
    // exists — this should never legitimately happen, so it's still logged loudly, but the
    // capacity check now actually runs against FREE-tier defaults instead of being skipped.
    @Test
    void assertCapacity_NoSubscriptionFound_EvaluatesAgainstFreeTierDefaults() {
        when(subscriptionRepository.findByCompanyId(99L)).thenReturn(Optional.empty());
        when(companyRepository.findStorageUsedBytes(99L)).thenReturn(Optional.of(1_000_000L));
        when(planLimitsService.getEffectiveStorageLimitBytes(Optional.<CompanySubscription>empty())).thenReturn(250_000_000L);

        assertThatCode(() -> storageQuotaService.assertCapacity(99L, 500L)).doesNotThrowAnyException();

        verify(planLimitsService).getEffectiveStorageLimitBytes(Optional.<CompanySubscription>empty());
        assertThat(logAppender.list)
                .anyMatch(event -> event.getLevel() == Level.ERROR
                        && event.getFormattedMessage().contains("no CompanySubscription"));
    }

    @Test
    void assertCapacity_NoSubscriptionFound_ThrowsWhenFreeTierLimitReached() {
        when(subscriptionRepository.findByCompanyId(99L)).thenReturn(Optional.empty());
        when(companyRepository.findStorageUsedBytes(99L)).thenReturn(Optional.of(249_000_000L));
        when(planLimitsService.getEffectiveStorageLimitBytes(Optional.<CompanySubscription>empty())).thenReturn(250_000_000L);

        assertThatThrownBy(() -> storageQuotaService.assertCapacity(99L, 5_000_000L))
                .isInstanceOf(StorageLimitExceededException.class);
    }
}
