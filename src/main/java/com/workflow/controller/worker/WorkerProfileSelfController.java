package com.workflow.controller.worker;

import java.io.IOException;
import java.time.LocalDate;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.workflow.dto.worker.WorkerProfileResponse;
import com.workflow.dto.worker.WorkerWeeklyHoursResponse;
import com.workflow.entity.auth.User;
import com.workflow.service.worker.IWorkerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Worker Profile (Self-Service)", description = "Endpoints for workers to manage their own profile")
@RestController
@RequestMapping("/api/v1/worker/profile")
@RequiredArgsConstructor
public class WorkerProfileSelfController {

    private final IWorkerService workerService;

    private Long getUserId(Authentication auth) {
        return ((User) auth.getPrincipal()).getId();
    }

    @Operation(summary = "Get the current worker's own profile")
    @GetMapping
    public ResponseEntity<WorkerProfileResponse> getOwnProfile(Authentication auth) {
        return ResponseEntity.ok(workerService.getOwnProfile(getUserId(auth)));
    }

    @Operation(summary = "Upload/replace the current worker's own profile photo")
    @PostMapping(value = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<WorkerProfileResponse> uploadOwnPhoto(
            @RequestParam("file") MultipartFile file,
            Authentication auth) throws IOException {
        return ResponseEntity.ok(workerService.uploadOwnPhoto(getUserId(auth), file));
    }

    @Operation(summary = "Get the current worker's total worked hours for a given week")
    @GetMapping("/hours/weekly")
    public ResponseEntity<WorkerWeeklyHoursResponse> getOwnWeeklyHours(
            @RequestParam(required = false) LocalDate date,
            Authentication auth) {
        return ResponseEntity.ok(workerService.getOwnWeeklyHours(getUserId(auth), date));
    }
}
