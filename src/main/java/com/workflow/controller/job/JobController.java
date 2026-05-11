package com.workflow.controller.job;

import com.workflow.common.util.AuthUtils;
import com.workflow.dto.job.*;
import com.workflow.service.company.ICompanyService;
import com.workflow.service.job.IJobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "Jobs")
@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private final IJobService jobService;
    private final ICompanyService companyService;

    @PostMapping
    public ResponseEntity<JobResponse> create(
            @Valid @RequestBody JobCreateRequest request,
            Authentication auth
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(jobService.createJob(request, getCompanyId(auth)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<JobResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody JobUpdateRequest request,
            Authentication auth
    ) {
        return ResponseEntity.ok(
                jobService.updateJob(id, request, getCompanyId(auth))
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> get(
            @PathVariable Long id,
            Authentication auth
    ) {
        return ResponseEntity.ok(
                jobService.getJob(id, getCompanyId(auth))
        );
    }

    @GetMapping
    public ResponseEntity<List<JobResponse>> getAll(Authentication auth) {
        return ResponseEntity.ok(
                jobService.getAllJobs(getCompanyId(auth))
        );
    }

    @GetMapping("/templates/{templateId}")
    public ResponseEntity<List<JobResponse>> getJobsByTemplate(
            @PathVariable Long templateId,
            Authentication auth
    ) {
        return ResponseEntity.ok(
                jobService.getJobsByTemplate(templateId, getCompanyId(auth))
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            Authentication auth
    ) {
        jobService.deleteJob(id, getCompanyId(auth));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/archive")
    public ResponseEntity<Void> archive(
            @PathVariable Long id,
            Authentication auth
    ) {
        jobService.archiveJob(id, getCompanyId(auth));
        return ResponseEntity.noContent().build();
    }

    private Long getCompanyId(Authentication auth) {
        return AuthUtils.getCompanyId(auth, companyService);
    }
}


