package com.workflow.controller.job;

import com.workflow.common.constant.job.JobStatus;
import com.workflow.common.security.RequireCompanyRole;
import com.workflow.common.util.AuthUtils;
import com.workflow.dto.job.*;
import com.workflow.service.job.IJobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static com.workflow.common.constant.CompanyRole.*;

@Tag(name = "Jobs")
@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private final IJobService jobService;

    @RequireCompanyRole({ COMPANY_ADMIN, MANAGER, EDITOR })
    @PostMapping
    public ResponseEntity<JobResponse> create(
            @Valid @RequestBody JobCreateRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(jobService.createJob(request, getCompanyId()));
    }

    @RequireCompanyRole({ COMPANY_ADMIN, MANAGER, EDITOR })
    @PutMapping("/{id}")
    public ResponseEntity<JobResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody JobUpdateRequest request,
            Authentication auth) {
        return ResponseEntity.ok(jobService.updateJob(id, request, getCompanyId()));
    }

    @RequireCompanyRole({ COMPANY_ADMIN, MANAGER, EDITOR, VIEWER })
    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> get(
            @PathVariable Long id,
            Authentication auth) {
        return ResponseEntity.ok(jobService.getJob(id, getCompanyId()));
    }

    @RequireCompanyRole({ COMPANY_ADMIN, MANAGER, EDITOR, VIEWER })
    @GetMapping
    public ResponseEntity<Page<JobResponse>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String clientName,
            @RequestParam(required = false) String workflowName,
            @RequestParam(required = false) String templateName,
            @RequestParam(required = false) JobStatus status,
            @RequestParam(required = false) Boolean archived,
            @RequestParam(required = false) BigDecimal minNet,
            @RequestParam(required = false) BigDecimal maxNet,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Pageable pageable,
            Authentication auth) {
        // Pass everything down to the service
        Page<JobResponse> responses = jobService.searchJobs(
                getCompanyId(),
                search,
                customerName,
                clientName,
                workflowName,
                templateName,
                status,
                archived,
                minNet,
                maxNet,
                startDate,
                endDate,
                pageable);

        return ResponseEntity.ok(responses);
    }

    @RequireCompanyRole({ COMPANY_ADMIN, MANAGER, EDITOR, VIEWER })
    @GetMapping("/archived")
    public ResponseEntity<List<JobResponse>> getArchived(Authentication auth) {
        return ResponseEntity.ok(jobService.getArchivedJobs(getCompanyId()));
    }

    @RequireCompanyRole({ COMPANY_ADMIN, MANAGER, EDITOR, VIEWER })
    @GetMapping("/templates/{templateId}")
    public ResponseEntity<List<JobResponse>> getJobsByTemplate(
            @PathVariable Long templateId,
            Authentication auth) {
        return ResponseEntity.ok(jobService.getJobsByTemplate(templateId, getCompanyId()));
    }

    @RequireCompanyRole({ COMPANY_ADMIN, MANAGER })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            Authentication auth) {
        jobService.deleteJob(id, getCompanyId());
        return ResponseEntity.noContent().build();
    }

    @RequireCompanyRole({ COMPANY_ADMIN, MANAGER })
    @PatchMapping("/{id}/archive")
    public ResponseEntity<Void> archive(
            @PathVariable Long id,
            Authentication auth) {
        jobService.archiveJob(id, getCompanyId());
        return ResponseEntity.noContent().build();
    }

    private Long getCompanyId() {
        return AuthUtils.getCompanyId();
    }
}
