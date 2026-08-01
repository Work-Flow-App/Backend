package com.workflow.controller.worker;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.workflow.dto.worker.LeaveRequestCreateRequest;
import com.workflow.dto.worker.LeaveRequestResponse;
import com.workflow.dto.worker.LeaveRequestUpdateRequest;
import com.workflow.entity.auth.User;
import com.workflow.service.worker.IWorkerLeaveRequestService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Worker Leave Requests (Self-Service)", description = "Endpoints for workers to manage their own leave requests")
@RestController
@RequestMapping("/api/v1/worker/leave-requests")
@RequiredArgsConstructor
public class WorkerLeaveRequestSelfController {

    private final IWorkerLeaveRequestService leaveRequestService;

    private Long getUserId(Authentication auth) {
        return ((User) auth.getPrincipal()).getId();
    }

    @Operation(summary = "Submit a leave request")
    @PostMapping
    public ResponseEntity<LeaveRequestResponse> submitLeaveRequest(
            @Valid @RequestBody LeaveRequestCreateRequest request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(leaveRequestService.submitOwnLeaveRequest(getUserId(auth), request));
    }

    @Operation(summary = "List the current worker's leave requests")
    @GetMapping
    public ResponseEntity<List<LeaveRequestResponse>> getOwnLeaveRequests(Authentication auth) {
        return ResponseEntity.ok(leaveRequestService.getOwnLeaveRequests(getUserId(auth)));
    }

    @Operation(summary = "Get a single own leave request")
    @GetMapping("/{id}")
    public ResponseEntity<LeaveRequestResponse> getOwnLeaveRequest(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(leaveRequestService.getOwnLeaveRequest(getUserId(auth), id));
    }

    @Operation(summary = "Edit a pending leave request")
    @PatchMapping("/{id}")
    public ResponseEntity<LeaveRequestResponse> updateOwnLeaveRequest(
            @PathVariable Long id,
            @Valid @RequestBody LeaveRequestUpdateRequest request,
            Authentication auth) {
        return ResponseEntity.ok(leaveRequestService.updateOwnLeaveRequest(getUserId(auth), id, request));
    }

    @Operation(summary = "Cancel a pending leave request")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelOwnLeaveRequest(@PathVariable Long id, Authentication auth) {
        leaveRequestService.cancelOwnLeaveRequest(getUserId(auth), id);
        return ResponseEntity.noContent().build();
    }
}
