package com.workflow.service.worker;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workflow.common.constant.worker.LeaveRequestStatus;
import com.workflow.common.exception.business.InvalidLeaveRequestStateException;
import com.workflow.common.exception.business.InvalidRequestException;
import com.workflow.common.exception.business.LeaveRequestNotFoundException;
import com.workflow.common.exception.business.LeaveRequestOverlapException;
import com.workflow.common.exception.business.WorkerNotFoundException;
import com.workflow.dto.worker.LeaveCalendarEntryResponse;
import com.workflow.dto.worker.LeaveRequestCreateRequest;
import com.workflow.dto.worker.LeaveRequestDecisionRequest;
import com.workflow.dto.worker.LeaveRequestResponse;
import com.workflow.dto.worker.LeaveRequestUpdateRequest;
import com.workflow.entity.auth.User;
import com.workflow.entity.worker.Worker;
import com.workflow.entity.worker.WorkerLeaveRequest;
import com.workflow.repository.worker.WorkerLeaveRequestRepository;
import com.workflow.repository.worker.WorkerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkerLeaveRequestService implements IWorkerLeaveRequestService {

    private final WorkerRepository workerRepository;
    private final WorkerLeaveRequestRepository leaveRequestRepository;

    private Worker getWorker(Long userId) {
        return workerRepository.findByUserId(userId)
                .orElseThrow(() -> new WorkerNotFoundException("Current user is not a registered worker"));
    }

    private void checkDates(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new InvalidRequestException("End date cannot be before start date");
        }
    }

    private void checkNoOverlap(Long workerId, LocalDate startDate, LocalDate endDate, Long excludeId) {
        if (!leaveRequestRepository.findOverlapping(workerId, startDate, endDate, excludeId).isEmpty()) {
            throw new LeaveRequestOverlapException("Leave request overlaps with an existing pending or approved request");
        }
    }

    /**
     * Locks the worker row so two concurrent submit/update calls for the same worker
     * can't both pass the overlap check before either commits (check-then-act race).
     */
    private void lockWorker(Long workerId) {
        workerRepository.findByIdForUpdate(workerId);
    }

    @Override
    public LeaveRequestResponse submitOwnLeaveRequest(Long workerUserId, LeaveRequestCreateRequest request) {
        Worker worker = getWorker(workerUserId);
        lockWorker(worker.getId());

        checkDates(request.startDate(), request.endDate());
        checkNoOverlap(worker.getId(), request.startDate(), request.endDate(), null);

        WorkerLeaveRequest leaveRequest = leaveRequestRepository.save(
                WorkerLeaveRequest.builder()
                        .worker(worker)
                        .leaveType(request.leaveType())
                        .startDate(request.startDate())
                        .endDate(request.endDate())
                        .reason(request.reason())
                        .status(LeaveRequestStatus.PENDING)
                        .build());

        return map(leaveRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> getOwnLeaveRequests(Long workerUserId) {
        Worker worker = getWorker(workerUserId);
        return leaveRequestRepository.findByWorkerIdOrderByStartDateDesc(worker.getId())
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LeaveRequestResponse getOwnLeaveRequest(Long workerUserId, Long leaveRequestId) {
        Worker worker = getWorker(workerUserId);
        return map(getOwnedLeaveRequest(leaveRequestId, worker.getId()));
    }

    @Override
    public LeaveRequestResponse updateOwnLeaveRequest(Long workerUserId, Long leaveRequestId, LeaveRequestUpdateRequest request) {
        Worker worker = getWorker(workerUserId);
        WorkerLeaveRequest leaveRequest = getOwnedLeaveRequest(leaveRequestId, worker.getId());

        if (leaveRequest.getStatus() != LeaveRequestStatus.PENDING) {
            throw new InvalidLeaveRequestStateException("Only pending leave requests can be edited");
        }

        lockWorker(worker.getId());
        checkDates(request.startDate(), request.endDate());
        checkNoOverlap(worker.getId(), request.startDate(), request.endDate(), leaveRequest.getId());

        leaveRequest.setStartDate(request.startDate());
        leaveRequest.setEndDate(request.endDate());
        if (request.reason() != null) {
            leaveRequest.setReason(request.reason());
        }

        return map(leaveRequestRepository.save(leaveRequest));
    }

    @Override
    public void cancelOwnLeaveRequest(Long workerUserId, Long leaveRequestId) {
        Worker worker = getWorker(workerUserId);
        WorkerLeaveRequest leaveRequest = getOwnedLeaveRequest(leaveRequestId, worker.getId());

        if (leaveRequest.getStatus() != LeaveRequestStatus.PENDING) {
            throw new InvalidLeaveRequestStateException("Only pending leave requests can be cancelled");
        }

        leaveRequest.setStatus(LeaveRequestStatus.CANCELLED);
        leaveRequestRepository.save(leaveRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LeaveRequestResponse> listCompanyLeaveRequests(Long companyId, LeaveRequestStatus status, Pageable pageable) {
        return leaveRequestRepository.findByCompanyId(companyId, status, pageable).map(this::map);
    }

    @Override
    @Transactional(readOnly = true)
    public LeaveRequestResponse getLeaveRequest(Long leaveRequestId, Long companyId) {
        return map(getCompanyLeaveRequest(leaveRequestId, companyId));
    }

    @Override
    public LeaveRequestResponse approveLeaveRequest(Long leaveRequestId, Long companyId, User admin, LeaveRequestDecisionRequest request) {
        WorkerLeaveRequest leaveRequest = getCompanyLeaveRequest(leaveRequestId, companyId);

        if (leaveRequest.getStatus() != LeaveRequestStatus.PENDING) {
            throw new InvalidLeaveRequestStateException("Only pending leave requests can be approved");
        }

        leaveRequest.setStatus(LeaveRequestStatus.APPROVED);
        leaveRequest.setDecisionBy(admin);
        leaveRequest.setDecisionAt(LocalDateTime.now());
        leaveRequest.setDecisionNote(request.decisionNote());

        return map(leaveRequestRepository.save(leaveRequest));
    }

    @Override
    public LeaveRequestResponse rejectLeaveRequest(Long leaveRequestId, Long companyId, User admin, LeaveRequestDecisionRequest request) {
        WorkerLeaveRequest leaveRequest = getCompanyLeaveRequest(leaveRequestId, companyId);

        if (leaveRequest.getStatus() != LeaveRequestStatus.PENDING) {
            throw new InvalidLeaveRequestStateException("Only pending leave requests can be rejected");
        }

        if (request.decisionNote() == null || request.decisionNote().isBlank()) {
            throw new InvalidRequestException("A decision note is required when rejecting a leave request");
        }

        leaveRequest.setStatus(LeaveRequestStatus.REJECTED);
        leaveRequest.setDecisionBy(admin);
        leaveRequest.setDecisionAt(LocalDateTime.now());
        leaveRequest.setDecisionNote(request.decisionNote());

        return map(leaveRequestRepository.save(leaveRequest));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveCalendarEntryResponse> getCompanyLeaveCalendar(Long companyId, LocalDate from, LocalDate to) {
        return leaveRequestRepository.findApprovedInRange(companyId, from, to)
                .stream()
                .map(lr -> new LeaveCalendarEntryResponse(
                        lr.getWorker().getId(),
                        lr.getWorker().getName(),
                        lr.getLeaveType(),
                        lr.getStartDate(),
                        lr.getEndDate()))
                .toList();
    }

    private WorkerLeaveRequest getOwnedLeaveRequest(Long leaveRequestId, Long workerId) {
        return leaveRequestRepository.findByIdAndWorkerId(leaveRequestId, workerId)
                .orElseThrow(() -> new LeaveRequestNotFoundException("Leave request not found with ID: " + leaveRequestId));
    }

    private WorkerLeaveRequest getCompanyLeaveRequest(Long leaveRequestId, Long companyId) {
        return leaveRequestRepository.findByIdAndWorkerCompanyId(leaveRequestId, companyId)
                .orElseThrow(() -> new LeaveRequestNotFoundException("Leave request not found with ID: " + leaveRequestId));
    }

    private LeaveRequestResponse map(WorkerLeaveRequest lr) {
        return new LeaveRequestResponse(
                lr.getId(),
                lr.getWorker().getId(),
                lr.getWorker().getName(),
                lr.getLeaveType(),
                lr.getStartDate(),
                lr.getEndDate(),
                lr.getReason(),
                lr.getStatus(),
                lr.getDecisionBy() == null ? null : lr.getDecisionBy().getUsername(),
                lr.getDecisionAt(),
                lr.getDecisionNote(),
                lr.getCreatedAt(),
                lr.getUpdatedAt());
    }
}
