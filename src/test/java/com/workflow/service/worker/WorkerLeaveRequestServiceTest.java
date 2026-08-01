package com.workflow.service.worker;

import com.workflow.common.constant.Role;
import com.workflow.common.constant.worker.LeaveRequestStatus;
import com.workflow.common.constant.worker.LeaveType;
import com.workflow.common.exception.business.InvalidLeaveRequestStateException;
import com.workflow.common.exception.business.InvalidRequestException;
import com.workflow.common.exception.business.LeaveRequestOverlapException;
import com.workflow.dto.worker.LeaveRequestCreateRequest;
import com.workflow.dto.worker.LeaveRequestDecisionRequest;
import com.workflow.dto.worker.LeaveRequestResponse;
import com.workflow.dto.worker.LeaveRequestUpdateRequest;
import com.workflow.entity.company.Company;
import com.workflow.entity.auth.User;
import com.workflow.entity.worker.Worker;
import com.workflow.entity.worker.WorkerLeaveRequest;
import com.workflow.repository.worker.WorkerLeaveRequestRepository;
import com.workflow.repository.worker.WorkerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkerLeaveRequestServiceTest {

    @Mock
    private WorkerRepository workerRepository;

    @Mock
    private WorkerLeaveRequestRepository leaveRequestRepository;

    @InjectMocks
    private WorkerLeaveRequestService leaveRequestService;

    private User workerUser;
    private User adminUser;
    private Worker worker;
    private WorkerLeaveRequest pendingRequest;

    @BeforeEach
    void setUp() {
        User companyUser = User.builder().id(1L).uuid("company-uuid").username("companyowner")
                .role(Role.COMPANY).enabled(true).build();
        Company company = Company.builder().id(1L).name("Test Company").user(companyUser).build();

        workerUser = User.builder().id(2L).uuid("worker-uuid").username("worker1")
                .role(Role.WORKER).enabled(true).build();
        adminUser = User.builder().id(3L).uuid("admin-uuid").username("admin1")
                .role(Role.COMPANY).enabled(true).build();

        worker = Worker.builder().id(10L).name("John Worker").company(company).user(workerUser)
                .archived(false).build();

        pendingRequest = WorkerLeaveRequest.builder()
                .id(50L)
                .worker(worker)
                .leaveType(LeaveType.ANNUAL)
                .startDate(LocalDate.of(2026, 8, 1))
                .endDate(LocalDate.of(2026, 8, 5))
                .reason("Family trip")
                .status(LeaveRequestStatus.PENDING)
                .build();
    }

    // ============= submitOwnLeaveRequest Tests =============

    @Test
    void submitOwnLeaveRequest_ShouldSubmitSuccessfully() {
        when(workerRepository.findByUserId(2L)).thenReturn(Optional.of(worker));
        when(leaveRequestRepository.findOverlapping(eq(10L), any(), any(), isNull())).thenReturn(List.of());
        when(leaveRequestRepository.save(any(WorkerLeaveRequest.class))).thenReturn(pendingRequest);

        LeaveRequestCreateRequest request = new LeaveRequestCreateRequest(
                LeaveType.ANNUAL, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 5), "Family trip");

        LeaveRequestResponse response = leaveRequestService.submitOwnLeaveRequest(2L, request);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(LeaveRequestStatus.PENDING);
        verify(leaveRequestRepository).save(any(WorkerLeaveRequest.class));
    }

    @Test
    void submitOwnLeaveRequest_ShouldLockWorkerRow_BeforeCheckingOverlap() {
        // Regression test: the overlap check must be inside a pessimistic lock on the
        // worker row so two concurrent submissions can't both pass the check.
        when(workerRepository.findByUserId(2L)).thenReturn(Optional.of(worker));
        when(leaveRequestRepository.findOverlapping(eq(10L), any(), any(), isNull())).thenReturn(List.of());
        when(leaveRequestRepository.save(any(WorkerLeaveRequest.class))).thenReturn(pendingRequest);

        LeaveRequestCreateRequest request = new LeaveRequestCreateRequest(
                LeaveType.ANNUAL, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 5), "Family trip");

        leaveRequestService.submitOwnLeaveRequest(2L, request);

        verify(workerRepository).findByIdForUpdate(10L);
    }

    @Test
    void submitOwnLeaveRequest_ShouldThrow_WhenEndDateBeforeStartDate() {
        when(workerRepository.findByUserId(2L)).thenReturn(Optional.of(worker));

        LeaveRequestCreateRequest request = new LeaveRequestCreateRequest(
                LeaveType.ANNUAL, LocalDate.of(2026, 8, 5), LocalDate.of(2026, 8, 1), null);

        assertThatThrownBy(() -> leaveRequestService.submitOwnLeaveRequest(2L, request))
                .isInstanceOf(InvalidRequestException.class);

        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    void submitOwnLeaveRequest_ShouldThrow_WhenOverlapsExistingRequest() {
        when(workerRepository.findByUserId(2L)).thenReturn(Optional.of(worker));
        when(leaveRequestRepository.findOverlapping(eq(10L), any(), any(), isNull()))
                .thenReturn(List.of(pendingRequest));

        LeaveRequestCreateRequest request = new LeaveRequestCreateRequest(
                LeaveType.ANNUAL, LocalDate.of(2026, 8, 2), LocalDate.of(2026, 8, 3), null);

        assertThatThrownBy(() -> leaveRequestService.submitOwnLeaveRequest(2L, request))
                .isInstanceOf(LeaveRequestOverlapException.class);

        verify(leaveRequestRepository, never()).save(any());
    }

    // ============= updateOwnLeaveRequest Tests =============

    @Test
    void updateOwnLeaveRequest_ShouldKeepExistingReason_WhenReasonNotProvided() {
        // Regression test: a partial PATCH (no reason field) must not wipe the
        // existing reason - only startDate/endDate should change.
        when(workerRepository.findByUserId(2L)).thenReturn(Optional.of(worker));
        when(leaveRequestRepository.findByIdAndWorkerId(50L, 10L)).thenReturn(Optional.of(pendingRequest));
        when(leaveRequestRepository.findOverlapping(eq(10L), any(), any(), eq(50L))).thenReturn(List.of());
        when(leaveRequestRepository.save(any(WorkerLeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        LeaveRequestUpdateRequest request = new LeaveRequestUpdateRequest(
                LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 12), null);

        LeaveRequestResponse response = leaveRequestService.updateOwnLeaveRequest(2L, 50L, request);

        assertThat(response.reason()).isEqualTo("Family trip");
        assertThat(response.startDate()).isEqualTo(LocalDate.of(2026, 8, 10));
    }

    @Test
    void updateOwnLeaveRequest_ShouldThrow_WhenNotPending() {
        pendingRequest.setStatus(LeaveRequestStatus.APPROVED);
        when(workerRepository.findByUserId(2L)).thenReturn(Optional.of(worker));
        when(leaveRequestRepository.findByIdAndWorkerId(50L, 10L)).thenReturn(Optional.of(pendingRequest));

        LeaveRequestUpdateRequest request = new LeaveRequestUpdateRequest(
                LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 12), null);

        assertThatThrownBy(() -> leaveRequestService.updateOwnLeaveRequest(2L, 50L, request))
                .isInstanceOf(InvalidLeaveRequestStateException.class);

        verify(leaveRequestRepository, never()).save(any());
    }

    // ============= cancelOwnLeaveRequest Tests =============

    @Test
    void cancelOwnLeaveRequest_ShouldThrow_WhenNotPending() {
        pendingRequest.setStatus(LeaveRequestStatus.REJECTED);
        when(workerRepository.findByUserId(2L)).thenReturn(Optional.of(worker));
        when(leaveRequestRepository.findByIdAndWorkerId(50L, 10L)).thenReturn(Optional.of(pendingRequest));

        assertThatThrownBy(() -> leaveRequestService.cancelOwnLeaveRequest(2L, 50L))
                .isInstanceOf(InvalidLeaveRequestStateException.class);

        verify(leaveRequestRepository, never()).save(any());
    }

    // ============= approveLeaveRequest / rejectLeaveRequest Tests =============

    @Test
    void approveLeaveRequest_ShouldApproveSuccessfully() {
        when(leaveRequestRepository.findByIdAndWorkerCompanyId(50L, 1L)).thenReturn(Optional.of(pendingRequest));
        when(leaveRequestRepository.save(any(WorkerLeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        LeaveRequestResponse response = leaveRequestService.approveLeaveRequest(
                50L, 1L, adminUser, new LeaveRequestDecisionRequest(null));

        assertThat(response.status()).isEqualTo(LeaveRequestStatus.APPROVED);
        assertThat(response.decisionByUsername()).isEqualTo("admin1");
    }

    @Test
    void approveLeaveRequest_ShouldThrow_WhenNotPending() {
        pendingRequest.setStatus(LeaveRequestStatus.CANCELLED);
        when(leaveRequestRepository.findByIdAndWorkerCompanyId(50L, 1L)).thenReturn(Optional.of(pendingRequest));

        assertThatThrownBy(() -> leaveRequestService.approveLeaveRequest(
                50L, 1L, adminUser, new LeaveRequestDecisionRequest(null)))
                .isInstanceOf(InvalidLeaveRequestStateException.class);

        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    void rejectLeaveRequest_ShouldThrow_WhenDecisionNoteBlank() {
        when(leaveRequestRepository.findByIdAndWorkerCompanyId(50L, 1L)).thenReturn(Optional.of(pendingRequest));

        assertThatThrownBy(() -> leaveRequestService.rejectLeaveRequest(
                50L, 1L, adminUser, new LeaveRequestDecisionRequest("  ")))
                .isInstanceOf(InvalidRequestException.class);

        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    void rejectLeaveRequest_ShouldRejectSuccessfully_WhenNoteProvided() {
        when(leaveRequestRepository.findByIdAndWorkerCompanyId(50L, 1L)).thenReturn(Optional.of(pendingRequest));
        when(leaveRequestRepository.save(any(WorkerLeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        LeaveRequestResponse response = leaveRequestService.rejectLeaveRequest(
                50L, 1L, adminUser, new LeaveRequestDecisionRequest("Team is short-staffed that week"));

        assertThat(response.status()).isEqualTo(LeaveRequestStatus.REJECTED);
        assertThat(response.decisionNote()).isEqualTo("Team is short-staffed that week");
    }
}
