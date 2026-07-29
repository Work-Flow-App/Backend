package com.workflow.service.worker;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.workflow.common.constant.worker.LeaveRequestStatus;
import com.workflow.dto.worker.LeaveCalendarEntryResponse;
import com.workflow.dto.worker.LeaveRequestCreateRequest;
import com.workflow.dto.worker.LeaveRequestDecisionRequest;
import com.workflow.dto.worker.LeaveRequestResponse;
import com.workflow.dto.worker.LeaveRequestUpdateRequest;
import com.workflow.entity.auth.User;

public interface IWorkerLeaveRequestService {

    // Self-service (worker managing their own leave requests)
    LeaveRequestResponse submitOwnLeaveRequest(Long workerUserId, LeaveRequestCreateRequest request);

    List<LeaveRequestResponse> getOwnLeaveRequests(Long workerUserId);

    LeaveRequestResponse getOwnLeaveRequest(Long workerUserId, Long leaveRequestId);

    LeaveRequestResponse updateOwnLeaveRequest(Long workerUserId, Long leaveRequestId, LeaveRequestUpdateRequest request);

    void cancelOwnLeaveRequest(Long workerUserId, Long leaveRequestId);

    // Admin/company-facing
    Page<LeaveRequestResponse> listCompanyLeaveRequests(Long companyId, LeaveRequestStatus status, Pageable pageable);

    LeaveRequestResponse getLeaveRequest(Long leaveRequestId, Long companyId);

    LeaveRequestResponse approveLeaveRequest(Long leaveRequestId, Long companyId, User admin, LeaveRequestDecisionRequest request);

    LeaveRequestResponse rejectLeaveRequest(Long leaveRequestId, Long companyId, User admin, LeaveRequestDecisionRequest request);

    List<LeaveCalendarEntryResponse> getCompanyLeaveCalendar(Long companyId, LocalDate from, LocalDate to);
}
