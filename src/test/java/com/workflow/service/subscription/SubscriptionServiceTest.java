package com.workflow.service.subscription;

import com.workflow.common.constant.PlanType;
import com.workflow.common.constant.SubscriptionStatus;
import com.workflow.common.exception.business.InvalidRequestException;
import com.workflow.common.exception.business.SeatReductionConflictException;
import com.workflow.common.exception.business.StorageReductionConflictException;
import com.workflow.common.exception.business.SubscriptionRequiredException;
import com.workflow.config.properties.PaddleConfigProperties;
import com.workflow.dto.paddle.PaddleSubscriptionResponse;
import com.workflow.entity.company.CompanySubscription;
import com.workflow.repository.company.CompanyRepository;
import com.workflow.repository.company.CompanySubscriptionRepository;
import com.workflow.repository.worker.WorkerInvitationRepository;
import com.workflow.repository.worker.WorkerRepository;
import com.workflow.service.paddle.IPaddleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private CompanySubscriptionRepository subscriptionRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private WorkerRepository workerRepository;

    @Mock
    private WorkerInvitationRepository invitationRepository;

    @Mock
    private IPlanLimitsService planLimitsService;

    @Mock
    private IPaddleService paddleService;

    @Mock
    private PaddleConfigProperties paddleProps;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private static final Long COMPANY_ID = 1L;

    private CompanySubscription activeSubscription() {
        return CompanySubscription.builder()
                .id(10L)
                .status(SubscriptionStatus.ACTIVE)
                .planType(PlanType.STARTER)
                .paddleSubscriptionId("sub_123")
                .extraUserSeats(2)
                .extraStorageBlocks(1)
                .build();
    }

    // ============= happy path =============

    @Test
    void updateAddons_BothChanged_CallsPaddleThenWritesLocallyAndReturnsResult() {
        CompanySubscription sub = activeSubscription();
        when(subscriptionRepository.findByCompanyIdForUpdate(COMPANY_ID)).thenReturn(Optional.of(sub));
        when(paddleService.updateSubscriptionAddons("sub_123", PlanType.STARTER, 5, 3))
                .thenReturn(new PaddleSubscriptionResponse(null));

        var result = subscriptionService.updateAddons(COMPANY_ID, 5, 3);

        assertThat(result.extraUserSeats()).isEqualTo(5);
        assertThat(result.extraStorageBlocks()).isEqualTo(3);
        assertThat(result.planType()).isEqualTo(PlanType.STARTER);
        assertThat(result.status()).isEqualTo(SubscriptionStatus.ACTIVE);

        verify(paddleService).updateSubscriptionAddons("sub_123", PlanType.STARTER, 5, 3);
        verify(subscriptionRepository).updateAddonsOptimistic(10L, 5, 3);
    }

    @Test
    void updateAddons_UsesPessimisticLock_NotPlainFindByCompanyId() {
        CompanySubscription sub = activeSubscription();
        when(subscriptionRepository.findByCompanyIdForUpdate(COMPANY_ID)).thenReturn(Optional.of(sub));
        when(paddleService.updateSubscriptionAddons(any(), any(), anyInt(), anyInt()))
                .thenReturn(new PaddleSubscriptionResponse(null));

        subscriptionService.updateAddons(COMPANY_ID, 5, 3);

        verify(subscriptionRepository).findByCompanyIdForUpdate(COMPANY_ID);
        verify(subscriptionRepository, never()).findByCompanyId(anyLong());
    }

    @Test
    void updateAddons_OnlySeatsProvided_StorageLeftUnchanged() {
        CompanySubscription sub = activeSubscription(); // extraUserSeats=2, extraStorageBlocks=1
        when(subscriptionRepository.findByCompanyIdForUpdate(COMPANY_ID)).thenReturn(Optional.of(sub));
        when(paddleService.updateSubscriptionAddons("sub_123", PlanType.STARTER, 7, 1))
                .thenReturn(new PaddleSubscriptionResponse(null));

        var result = subscriptionService.updateAddons(COMPANY_ID, 7, null);

        assertThat(result.extraUserSeats()).isEqualTo(7);
        assertThat(result.extraStorageBlocks()).isEqualTo(1);
        verify(paddleService).updateSubscriptionAddons("sub_123", PlanType.STARTER, 7, 1);
        verify(subscriptionRepository).updateAddonsOptimistic(10L, 7, 1);
    }

    // ============= no-op short-circuit =============

    @Test
    void updateAddons_RequestedValuesMatchCurrent_DoesNotCallPaddleOrWriteLocally() {
        CompanySubscription sub = activeSubscription(); // extraUserSeats=2, extraStorageBlocks=1
        when(subscriptionRepository.findByCompanyIdForUpdate(COMPANY_ID)).thenReturn(Optional.of(sub));

        var result = subscriptionService.updateAddons(COMPANY_ID, 2, 1);

        assertThat(result.extraUserSeats()).isEqualTo(2);
        assertThat(result.extraStorageBlocks()).isEqualTo(1);
        verifyNoInteractions(paddleService);
        verify(subscriptionRepository, never()).updateAddonsOptimistic(anyLong(), anyInt(), anyInt());
    }

    @Test
    void updateAddons_BothFieldsNull_IsNoOpAgainstCurrentState() {
        CompanySubscription sub = activeSubscription();
        when(subscriptionRepository.findByCompanyIdForUpdate(COMPANY_ID)).thenReturn(Optional.of(sub));

        var result = subscriptionService.updateAddons(COMPANY_ID, null, null);

        assertThat(result.extraUserSeats()).isEqualTo(2);
        assertThat(result.extraStorageBlocks()).isEqualTo(1);
        verifyNoInteractions(paddleService);
    }

    // ============= guard rails =============

    @Test
    void updateAddons_NoPaddleSubscriptionId_ThrowsInvalidRequestException() {
        CompanySubscription sub = activeSubscription();
        sub.setPaddleSubscriptionId(null);
        when(subscriptionRepository.findByCompanyIdForUpdate(COMPANY_ID)).thenReturn(Optional.of(sub));

        assertThatThrownBy(() -> subscriptionService.updateAddons(COMPANY_ID, 5, null))
                .isInstanceOf(InvalidRequestException.class);
        verifyNoInteractions(paddleService);
    }

    @Test
    void updateAddons_FreePlan_ThrowsInvalidRequestException() {
        CompanySubscription sub = activeSubscription();
        sub.setPlanType(PlanType.FREE);
        when(subscriptionRepository.findByCompanyIdForUpdate(COMPANY_ID)).thenReturn(Optional.of(sub));

        assertThatThrownBy(() -> subscriptionService.updateAddons(COMPANY_ID, 5, null))
                .isInstanceOf(InvalidRequestException.class);
        verifyNoInteractions(paddleService);
    }

    @Test
    void updateAddons_CancelledSubscription_ThrowsSubscriptionRequiredException() {
        CompanySubscription sub = activeSubscription();
        sub.setStatus(SubscriptionStatus.CANCELLED);
        when(subscriptionRepository.findByCompanyIdForUpdate(COMPANY_ID)).thenReturn(Optional.of(sub));

        assertThatThrownBy(() -> subscriptionService.updateAddons(COMPANY_ID, 5, null))
                .isInstanceOf(SubscriptionRequiredException.class);
        verifyNoInteractions(paddleService);
    }

    @Test
    void updateAddons_NoSubscriptionRecord_ThrowsInvalidRequestException() {
        when(subscriptionRepository.findByCompanyIdForUpdate(COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.updateAddons(COMPANY_ID, 5, null))
                .isInstanceOf(InvalidRequestException.class);
        verifyNoInteractions(paddleService);
    }

    // ============= decrease validation =============

    @Test
    void updateAddons_SeatDecreaseBelowInUseCount_ThrowsSeatReductionConflictException() {
        CompanySubscription sub = activeSubscription(); // extraUserSeats=2
        when(subscriptionRepository.findByCompanyIdForUpdate(COMPANY_ID)).thenReturn(Optional.of(sub));
        when(workerRepository.countByCompanyIdAndArchivedFalse(COMPANY_ID)).thenReturn(4L);
        when(invitationRepository.countPendingByCompanyId(anyLong(), any())).thenReturn(1L);
        when(planLimitsService.getEffectiveMaxUsers(PlanType.STARTER, 0)).thenReturn(3); // tier max 3 + 0 extra

        // 4 active + 1 pending = 5 in use, new max would be 3 -> conflict
        assertThatThrownBy(() -> subscriptionService.updateAddons(COMPANY_ID, 0, null))
                .isInstanceOf(SeatReductionConflictException.class);
        verifyNoInteractions(paddleService);
    }

    @Test
    void updateAddons_SeatDecreaseWithinUsage_Succeeds() {
        CompanySubscription sub = activeSubscription(); // extraUserSeats=2
        when(subscriptionRepository.findByCompanyIdForUpdate(COMPANY_ID)).thenReturn(Optional.of(sub));
        when(workerRepository.countByCompanyIdAndArchivedFalse(COMPANY_ID)).thenReturn(2L);
        when(invitationRepository.countPendingByCompanyId(anyLong(), any())).thenReturn(0L);
        when(planLimitsService.getEffectiveMaxUsers(PlanType.STARTER, 1)).thenReturn(4); // tier max 3 + 1 extra
        when(paddleService.updateSubscriptionAddons("sub_123", PlanType.STARTER, 1, 1))
                .thenReturn(new PaddleSubscriptionResponse(null));

        var result = subscriptionService.updateAddons(COMPANY_ID, 1, null);

        assertThat(result.extraUserSeats()).isEqualTo(1);
        verify(subscriptionRepository).updateAddonsOptimistic(10L, 1, 1);
    }

    @Test
    void updateAddons_StorageDecreaseBelowUsedBytes_ThrowsStorageReductionConflictException() {
        CompanySubscription sub = activeSubscription(); // extraStorageBlocks=1
        when(subscriptionRepository.findByCompanyIdForUpdate(COMPANY_ID)).thenReturn(Optional.of(sub));
        when(companyRepository.findStorageUsedBytes(COMPANY_ID)).thenReturn(Optional.of(11_000_000_000L));
        when(planLimitsService.getEffectiveStorageLimitBytes(PlanType.STARTER, 0)).thenReturn(10_000_000_000L);

        assertThatThrownBy(() -> subscriptionService.updateAddons(COMPANY_ID, null, 0))
                .isInstanceOf(StorageReductionConflictException.class);
        verifyNoInteractions(paddleService);
    }

    @Test
    void updateAddons_StorageDecreaseWithinUsage_Succeeds() {
        CompanySubscription sub = activeSubscription(); // extraStorageBlocks=1
        when(subscriptionRepository.findByCompanyIdForUpdate(COMPANY_ID)).thenReturn(Optional.of(sub));
        when(companyRepository.findStorageUsedBytes(COMPANY_ID)).thenReturn(Optional.of(5_000_000_000L));
        when(planLimitsService.getEffectiveStorageLimitBytes(eq(PlanType.STARTER), eq(0))).thenReturn(10_000_000_000L);
        when(paddleService.updateSubscriptionAddons("sub_123", PlanType.STARTER, 2, 0))
                .thenReturn(new PaddleSubscriptionResponse(null));

        var result = subscriptionService.updateAddons(COMPANY_ID, null, 0);

        assertThat(result.extraStorageBlocks()).isEqualTo(0);
        verify(subscriptionRepository).updateAddonsOptimistic(10L, 2, 0);
    }

    // ============= Paddle failure prevents local write =============

    @Test
    void updateAddons_PaddleCallThrows_LocalWriteNeverCalled() {
        CompanySubscription sub = activeSubscription();
        when(subscriptionRepository.findByCompanyIdForUpdate(COMPANY_ID)).thenReturn(Optional.of(sub));
        when(paddleService.updateSubscriptionAddons("sub_123", PlanType.STARTER, 5, 1))
                .thenThrow(new RuntimeException("Paddle API error"));

        assertThatThrownBy(() -> subscriptionService.updateAddons(COMPANY_ID, 5, null))
                .isInstanceOf(RuntimeException.class);

        verify(subscriptionRepository, never()).updateAddonsOptimistic(anyLong(), anyInt(), anyInt());
    }
}
