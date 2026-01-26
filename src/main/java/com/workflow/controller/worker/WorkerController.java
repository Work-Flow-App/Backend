package com.workflow.controller.worker;

import com.workflow.dto.worker.*;
import com.workflow.dto.worker.validators.PatchValidation;
import com.workflow.dto.worker.validators.PutValidation;
import com.workflow.entity.User;
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

@Tag(name = "Workers")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/workers")
public class WorkerController {

    private final IWorkerService workerService;
    private final WorkerInvitationService workerInvitationService;

    @PostMapping
    public ResponseEntity<WorkerResponse> createWorker(
            @Valid @RequestBody WorkerCreateRequest request,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        WorkerResponse response = workerService.createWorker(request, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<WorkerResponse>> getAllWorkers(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<WorkerResponse> workers = workerService.getAllWorkers(user.getId());
        return ResponseEntity.ok(workers);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkerResponse> getWorkerById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        WorkerResponse response = workerService.getWorkerById(id, user.getId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkerResponse> updateWorker(
            @PathVariable Long id,
            @Validated({PutValidation.class, Default.class}) @RequestBody WorkerUpdateRequest request,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        WorkerResponse response = workerService.updateWorker(id, request, user.getId());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<WorkerResponse> patchWorker(
            @PathVariable Long id,
            @Validated({PatchValidation.class, Default.class}) @RequestBody WorkerUpdateRequest request,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        WorkerResponse response = workerService.patchWorker(id, request, user.getId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorker(
            @PathVariable Long id,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        workerService.deleteWorker(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/invite")
    public ResponseEntity<WorkerInviteResponse> sendInvitation(
            @Valid @RequestBody WorkerInvitationRequest request,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        WorkerInviteResponse response = workerInvitationService.createInvitation(
                request.email(),
                user.getId()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/invites")
    public ResponseEntity<List<WorkerInvitationStatusResponse>> getInvitationStatus(
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        List<WorkerInvitationStatusResponse> invitations =
                workerInvitationService.getInvitationsByCompany(user.getId());
        return ResponseEntity.ok(invitations);
    }

    @GetMapping("/invites/check/{token}")
    public ResponseEntity<WorkerInvitationCheckResponse> checkInvitation(
            @PathVariable String token
    ) {
        WorkerInvitationCheckResponse response = workerInvitationService.checkInvitation(token);
        return ResponseEntity.ok(response);
    }
}