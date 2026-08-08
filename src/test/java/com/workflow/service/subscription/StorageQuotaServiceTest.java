package com.workflow.service.subscription;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.workflow.common.constant.PlanType;
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

    @Test
    void assertCapacity_WithinLimit_DoesNotLogWarning() {
        CompanySubscription subscription = CompanySubscription.builder().planType(PlanType.STARTER).build();
        when(subscriptionRepository.findByCompanyId(1L)).thenReturn(Optional.of(subscription));
        when(companyRepository.findStorageUsedBytes(1L)).thenReturn(Optional.of(1_000_000L));
        when(planLimitsService.getEffectiveStorageLimitBytes(subscription)).thenReturn(10_000_000_000L);

        storageQuotaService.assertCapacity(1L, 500_000L);

        assertThat(logAppender.list).noneMatch(event -> event.getLevel() == Level.WARN);
    }

    @Test
    void assertCapacity_ExceedsLimit_LogsWarningButDoesNotThrow() {
        CompanySubscription subscription = CompanySubscription.builder().planType(PlanType.FREE).build();
        when(subscriptionRepository.findByCompanyId(1L)).thenReturn(Optional.of(subscription));
        when(companyRepository.findStorageUsedBytes(1L)).thenReturn(Optional.of(249_000_000L));
        when(planLimitsService.getEffectiveStorageLimitBytes(subscription)).thenReturn(250_000_000L);

        // 249M used + 5M incoming > 250M limit — tracking only, must not throw
        assertThatCode(() -> storageQuotaService.assertCapacity(1L, 5_000_000L)).doesNotThrowAnyException();

        assertThat(logAppender.list)
                .anyMatch(event -> event.getLevel() == Level.WARN
                        && event.getFormattedMessage().contains("companyId=1"));
    }

    @Test
    void assertCapacity_ExactlyAtLimit_DoesNotLogWarning() {
        CompanySubscription subscription = CompanySubscription.builder().planType(PlanType.FREE).build();
        when(subscriptionRepository.findByCompanyId(1L)).thenReturn(Optional.of(subscription));
        when(companyRepository.findStorageUsedBytes(1L)).thenReturn(Optional.of(240_000_000L));
        when(planLimitsService.getEffectiveStorageLimitBytes(subscription)).thenReturn(250_000_000L);

        // 240M + 10M == 250M limit exactly — not over, should not warn
        storageQuotaService.assertCapacity(1L, 10_000_000L);

        assertThat(logAppender.list).noneMatch(event -> event.getLevel() == Level.WARN);
    }

    @Test
    void assertCapacity_NoUsageRecordedYet_TreatsCurrentUsageAsZero() {
        CompanySubscription subscription = CompanySubscription.builder().planType(PlanType.STARTER).build();
        when(subscriptionRepository.findByCompanyId(1L)).thenReturn(Optional.of(subscription));
        when(companyRepository.findStorageUsedBytes(1L)).thenReturn(Optional.empty());
        when(planLimitsService.getEffectiveStorageLimitBytes(subscription)).thenReturn(10_000_000_000L);

        storageQuotaService.assertCapacity(1L, 500L);

        assertThat(logAppender.list).noneMatch(event -> event.getLevel() == Level.WARN);
    }

    // Decision (Phase 2): a missing CompanySubscription must not throw out of an upload path —
    // this method is tracking-only in Phase 2 (enforcement lands in Phase 3), so a data anomaly
    // here degrades to "skip the check and log," never a hard failure.
    @Test
    void assertCapacity_NoSubscriptionFound_SkipsCheckGracefullyWithoutThrowing() {
        when(subscriptionRepository.findByCompanyId(99L)).thenReturn(Optional.empty());

        assertThatCode(() -> storageQuotaService.assertCapacity(99L, 500L)).doesNotThrowAnyException();

        verify(companyRepository, never()).findStorageUsedBytes(any());
        verifyNoInteractions(planLimitsService);
        assertThat(logAppender.list)
                .anyMatch(event -> event.getLevel() == Level.WARN
                        && event.getFormattedMessage().contains("no CompanySubscription"));
    }
}
