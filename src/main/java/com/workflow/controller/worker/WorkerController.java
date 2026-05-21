package com.workflow.controller.worker;

import com.workflow.common.security.RequireCompanyRole;
import com.workflow.dto.worker.*;
import com.workflow.dto.worker.validators.PatchValidation;
import com.workflow.dto.worker.validators.PutValidation;
import com.workflow.entity.auth.User;
import com.workflow.service.worker.IWorkerService;
import com.workflow.service.worker.WorkerInvitationService;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

import static com.workflow.common.constant.CompanyRole.*;

@Tag(name = "Workers")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/workers")
public class WorkerController {

    private final IWorkerService workerService;
    private final WorkerInvitationService workerInvitationService;

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER})
    @PostMapping
    public ResponseEntity<WorkerResponse> createWorker(
            @Valid @RequestBody WorkerCreateRequest request,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workerService.createWorker(request, user.getId()));
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR, VIEWER})
    @GetMapping
    public ResponseEntity<List<WorkerResponse>> getAllWorkers(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(workerService.getAllWorkers(user.getId()));
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR, VIEWER})
    @GetMapping("/{id}")
    public ResponseEntity<WorkerResponse> getWorkerById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(workerService.getWorkerById(id, user.getId()));
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER})
    @PutMapping("/{id}")
    public ResponseEntity<WorkerResponse> updateWorker(
            @PathVariable Long id,
            @Validated({PutValidation.class, Default.class}) @RequestBody WorkerUpdateRequest request,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(workerService.updateWorker(id, request, user.getId()));
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER})
    @PatchMapping("/{id}")
    public ResponseEntity<WorkerResponse> patchWorker(
            @PathVariable Long id,
            @Validated({PatchValidation.class, Default.class}) @RequestBody WorkerUpdateRequest request,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(workerService.patchWorker(id, request, user.getId()));
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorker(
            @PathVariable Long id,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        workerService.deleteWorker(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER})
    @PutMapping("/{id}/reset-password")
    public ResponseEntity<Void> resetWorkerUsernamePassword(
            @PathVariable Long id,
            @Valid @RequestBody WorkerPasswordResetRequest request,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        workerService.resetWorkerUsernamePassword(id, request, user.getId());
        return ResponseEntity.noContent().build();
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER})
    @PostMapping("/invite")
    public ResponseEntity<WorkerInviteResponse> sendInvitation(
            @Valid @RequestBody WorkerInvitationRequest request,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(workerInvitationService.createInvitation(request.email(), user.getId()));
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR, VIEWER})
    @GetMapping("/invites")
    public ResponseEntity<List<WorkerInvitationStatusResponse>> getInvitationStatus(
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(workerInvitationService.getInvitationsByCompany(user.getId()));
    }

    @GetMapping("/invites/check/{token}")
    public ResponseEntity<WorkerInvitationCheckResponse> checkInvitation(
            @PathVariable String token
    ) {
        return ResponseEntity.ok(workerInvitationService.checkInvitation(token));
    }
}
