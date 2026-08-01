package com.workflow.controller.worker;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.workflow.common.constant.worker.LeaveRequestStatus;
import com.workflow.common.security.RequireCompanyRole;
import com.workflow.common.util.AuthUtils;
import com.workflow.dto.worker.LeaveCalendarEntryResponse;
import com.workflow.dto.worker.LeaveRequestDecisionRequest;
import com.workflow.dto.worker.LeaveRequestResponse;
import com.workflow.entity.auth.User;
import com.workflow.service.worker.IWorkerLeaveRequestService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import static com.workflow.common.constant.CompanyRole.*;

@Tag(name = "Company Leave Requests", description = "Company-facing endpoints to review and decide worker leave requests")
@RestController
@RequestMapping("/api/v1/workers/leave-requests")
@RequiredArgsConstructor
public class CompanyLeaveRequestController {

    private final IWorkerLeaveRequestService leaveRequestService;

    private Long getCompanyId() {
        return AuthUtils.getCompanyId();
    }

    @Operation(summary = "List leave requests company-wide, optionally filtered by status")
    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR, VIEWER})
    @GetMapping
    public ResponseEntity<Page<LeaveRequestResponse>> listLeaveRequests(
            @RequestParam(required = false) LeaveRequestStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(leaveRequestService.listCompanyLeaveRequests(getCompanyId(), status, pageable));
    }

    @Operation(summary = "Get a single leave request")
    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR, VIEWER})
    @GetMapping("/{id}")
    public ResponseEntity<LeaveRequestResponse> getLeaveRequest(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(leaveRequestService.getLeaveRequest(id, getCompanyId()));
    }

    @Operation(summary = "Approve a pending leave request")
    @RequireCompanyRole({COMPANY_ADMIN, MANAGER})
    @PostMapping("/{id}/approve")
    public ResponseEntity<LeaveRequestResponse> approveLeaveRequest(
            @PathVariable Long id,
            @RequestBody(required = false) LeaveRequestDecisionRequest request,
            Authentication auth) {
        User admin = (User) auth.getPrincipal();
        LeaveRequestDecisionRequest body = request != null ? request : new LeaveRequestDecisionRequest(null);
        return ResponseEntity.ok(leaveRequestService.approveLeaveRequest(id, getCompanyId(), admin, body));
    }

    @Operation(summary = "Reject a pending leave request")
    @RequireCompanyRole({COMPANY_ADMIN, MANAGER})
    @PostMapping("/{id}/reject")
    public ResponseEntity<LeaveRequestResponse> rejectLeaveRequest(
            @PathVariable Long id,
            @Valid @RequestBody LeaveRequestDecisionRequest request,
            Authentication auth) {
        User admin = (User) auth.getPrincipal();
        return ResponseEntity.ok(leaveRequestService.rejectLeaveRequest(id, getCompanyId(), admin, request));
    }

    @Operation(summary = "Get approved leave calendar company-wide for a date range")
    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR, VIEWER})
    @GetMapping("/calendar")
    public ResponseEntity<List<LeaveCalendarEntryResponse>> getLeaveCalendar(
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            Authentication auth) {
        return ResponseEntity.ok(leaveRequestService.getCompanyLeaveCalendar(getCompanyId(), from, to));
    }
}
