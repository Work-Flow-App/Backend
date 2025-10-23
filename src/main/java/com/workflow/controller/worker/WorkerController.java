package com.workflow.controller.worker;

import com.workflow.dto.worker.WorkerCreateRequest;
import com.workflow.dto.worker.WorkerInviteResponse;
import com.workflow.dto.worker.WorkerResponse;
import com.workflow.dto.worker.WorkerUpdateRequest;
import com.workflow.entity.User;
import com.workflow.service.worker.IWorkerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/workers")
public class WorkerController {

    private final IWorkerService workerService;

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
            @Valid @RequestBody WorkerUpdateRequest request,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        WorkerResponse response = workerService.updateWorker(id, request, user.getId());
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

    @PostMapping("/{id}/invite")
    public ResponseEntity<WorkerInviteResponse> sendInvitation(
            @PathVariable Long id,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        WorkerInviteResponse response = workerService.sendInvitation(id, user.getId());
        return ResponseEntity.ok(response);
    }
}