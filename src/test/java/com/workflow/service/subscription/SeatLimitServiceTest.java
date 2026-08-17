package com.workflow.service.subscription;

import com.workflow.common.constant.PlanType;
import com.workflow.common.exception.business.SeatLimitExceededException;
import com.workflow.entity.company.CompanySubscription;
import com.workflow.repository.company.CompanySubscriptionRepository;
import com.workflow.repository.worker.WorkerInvitationRepository;
import com.workflow.repository.worker.WorkerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeatLimitServiceTest {

    @Mock
    private WorkerRepository workerRepository;

    @Mock
    private WorkerInvitationRepository invitationRepository;

    @Mock
    private CompanySubscriptionRepository subscriptionRepository;

    @Mock
    private IPlanLimitsService planLimitsService;

    @InjectMocks
    private SeatLimitService seatLimitService;

    @Test
    void assertCapacity_UnderLimit_DoesNotThrow() {
        CompanySubscription subscription = CompanySubscription.builder().planType(PlanType.STARTER).build();
        when(subscriptionRepository.findByCompanyIdForUpdate(1L)).thenReturn(Optional.of(subscription));
        when(workerRepository.countByCompanyIdAndArchivedFalse(1L)).thenReturn(1L);
        when(invitationRepository.countPendingByCompanyId(anyLong(), any())).thenReturn(0L);
        when(planLimitsService.getEffectiveMaxUsers(Optional.of(subscription))).thenReturn(3);

        assertThatCode(() -> seatLimitService.assertCapacity(1L)).doesNotThrowAnyException();
    }

    @Test
    void assertCapacity_ActiveWorkersAtLimit_Throws() {
        CompanySubscription subscription = CompanySubscription.builder().planType(PlanType.STARTER).build();
        when(subscriptionRepository.findByCompanyIdForUpdate(1L)).thenReturn(Optional.of(subscription));
        when(workerRepository.countByCompanyIdAndArchivedFalse(1L)).thenReturn(3L);
        when(invitationRepository.countPendingByCompanyId(anyLong(), any())).thenReturn(0L);
        when(planLimitsService.getEffectiveMaxUsers(Optional.of(subscription))).thenReturn(3);

        assertThatThrownBy(() -> seatLimitService.assertCapacity(1L))
                .isInstanceOf(SeatLimitExceededException.class);
    }

    @Test
    void assertCapacity_ActiveWorkersPlusPendingInvitesAtLimit_Throws() {
        // 2 active + 1 pending invite == 3 max — pending invites must count toward the cap,
        // otherwise a company could stack invitations past the limit and accept them all at once.
        CompanySubscription subscription = CompanySubscription.builder().planType(PlanType.STARTER).build();
        when(subscriptionRepository.findByCompanyIdForUpdate(1L)).thenReturn(Optional.of(subscription));
        when(workerRepository.countByCompanyIdAndArchivedFalse(1L)).thenReturn(2L);
        when(invitationRepository.countPendingByCompanyId(anyLong(), any())).thenReturn(1L);
        when(planLimitsService.getEffectiveMaxUsers(Optional.of(subscription))).thenReturn(3);

        assertThatThrownBy(() -> seatLimitService.assertCapacity(1L))
                .isInstanceOf(SeatLimitExceededException.class);
    }

    // Decision: fail CLOSED to FREE-tier limits (not open/unlimited) when no CompanySubscription
    // exists — this should never legitimately happen, so it's still logged loudly, but the seat
    // check now actually runs against FREE-tier defaults instead of being skipped.
    @Test
    void assertCapacity_NoSubscriptionFound_EvaluatesAgainstFreeTierDefaults() {
        when(subscriptionRepository.findByCompanyIdForUpdate(99L)).thenReturn(Optional.empty());
        when(workerRepository.countByCompanyIdAndArchivedFalse(99L)).thenReturn(0L);
        when(invitationRepository.countPendingByCompanyId(anyLong(), any())).thenReturn(0L);
        when(planLimitsService.getEffectiveMaxUsers(Optional.<CompanySubscription>empty())).thenReturn(1);

        assertThatCode(() -> seatLimitService.assertCapacity(99L)).doesNotThrowAnyException();

        verify(planLimitsService).getEffectiveMaxUsers(Optional.<CompanySubscription>empty());
    }

    @Test
    void assertCapacity_NoSubscriptionFound_ThrowsWhenFreeTierLimitReached() {
        when(subscriptionRepository.findByCompanyIdForUpdate(99L)).thenReturn(Optional.empty());
        when(workerRepository.countByCompanyIdAndArchivedFalse(99L)).thenReturn(1L);
        when(invitationRepository.countPendingByCompanyId(anyLong(), any())).thenReturn(0L);
        when(planLimitsService.getEffectiveMaxUsers(Optional.<CompanySubscription>empty())).thenReturn(1);

        assertThatThrownBy(() -> seatLimitService.assertCapacity(99L))
                .isInstanceOf(SeatLimitExceededException.class);
    }
}
