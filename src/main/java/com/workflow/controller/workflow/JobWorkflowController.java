package com.workflow.controller.workflow;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.workflow.common.security.RequireCompanyRole;
import com.workflow.common.util.AuthUtils;
import com.workflow.dto.workflow.JobWorkflowResponse;
import com.workflow.dto.workflow.JobWorkflowStepCreateRequest;
import com.workflow.dto.workflow.JobWorkflowStepResponse;
import com.workflow.dto.workflow.JobWorkflowStepUpdateRequest;
import com.workflow.dto.workflow.JobWorkflowUpdateRequest;
import com.workflow.service.workflow.IJobWorkflowService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import static com.workflow.common.constant.CompanyRole.*;

@Tag(name = "Job Workflows")
@RestController
@RequestMapping("/api/v1/job-workflows")
@RequiredArgsConstructor
public class JobWorkflowController {

        private final IJobWorkflowService jobWorkflowService;

        private Long getCompanyId() {
                return AuthUtils.getCompanyId();
        }

        @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR})
        @PostMapping("/jobs/{jobId}/workflows/{workflowId}/start")
        public ResponseEntity<JobWorkflowResponse> startWorkflow(
                        @PathVariable Long jobId,
                        @PathVariable Long workflowId,
                        Authentication auth) {
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(jobWorkflowService.startWorkflowForJob(jobId, workflowId, getCompanyId()));
        }

        @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR})
        @PutMapping("/{jobWorkflowId}")
        public ResponseEntity<JobWorkflowResponse> updateJobWorkflow(
                        @PathVariable Long jobWorkflowId,
                        @Valid @RequestBody JobWorkflowUpdateRequest request,
                        Authentication auth) {
                return ResponseEntity.ok(jobWorkflowService.updateJobWorkflowById(
                                jobWorkflowId, request, getCompanyId()));
        }

        @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR, VIEWER})
        @GetMapping("/jobs/{jobId}")
        public ResponseEntity<JobWorkflowResponse> getJobWorkflow(
                        @PathVariable Long jobId,
                        Authentication auth) {
                return ResponseEntity.ok(jobWorkflowService.getJobWorkflowByJobId(jobId, getCompanyId()));
        }

        @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR})
        @PutMapping("/{jobWorkflowId}/steps/{stepId}")
        public ResponseEntity<JobWorkflowStepResponse> updateStep(
                        @PathVariable Long jobWorkflowId,
                        @PathVariable Long stepId,
                        @Valid @RequestBody JobWorkflowStepUpdateRequest request,
                        Authentication auth) {
                return ResponseEntity.ok(jobWorkflowService.updateStep(
                                jobWorkflowId, stepId, request, getCompanyId()));
        }

        @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR, VIEWER})
        @GetMapping("/{jobWorkflowId}")
        public ResponseEntity<JobWorkflowResponse> getJobWorkflowById(
                        @PathVariable Long jobWorkflowId,
                        Authentication auth) {
                return ResponseEntity.ok(jobWorkflowService.getJobWorkflowById(jobWorkflowId, getCompanyId()));
        }

        @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR, VIEWER})
        @GetMapping
        public ResponseEntity<List<JobWorkflowResponse>> getAllJobWorkflows(Authentication auth) {
                return ResponseEntity.ok(jobWorkflowService.getAllJobWorkflows(getCompanyId()));
        }

        @RequireCompanyRole({COMPANY_ADMIN, MANAGER})
        @DeleteMapping("/jobs/{jobId}")
        public ResponseEntity<Void> deleteByJobId(
                        @PathVariable Long jobId,
                        Authentication auth) {
                jobWorkflowService.deleteByJobId(jobId, getCompanyId());
                return ResponseEntity.noContent().build();
        }

        @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR})
        @PutMapping("/{jobWorkflowId}/assign-worker/{workerId}")
        public ResponseEntity<JobWorkflowResponse> assignWorkerToAllSteps(
                        @PathVariable Long jobWorkflowId,
                        @PathVariable Long workerId,
                        Authentication auth) {
                return ResponseEntity.ok(jobWorkflowService.assignAWorkerToAllSteps(
                                jobWorkflowId, workerId, getCompanyId()));
        }

        @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR})
        @PutMapping("/{jobWorkflowId}/assign-workers")
        public ResponseEntity<JobWorkflowResponse> assignWorkersToAllSteps(
                        @PathVariable Long jobWorkflowId,
                        @RequestBody List<Long> workerIds,
                        Authentication auth) {
                return ResponseEntity.ok(jobWorkflowService.assignWorkersToAllSteps(
                                jobWorkflowId, workerIds, getCompanyId()));
        }

        @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR})
        @PostMapping("/{jobWorkflowId}/steps")
        public ResponseEntity<JobWorkflowStepResponse> addStep(
                        @PathVariable Long jobWorkflowId,
                        @Valid @RequestBody JobWorkflowStepCreateRequest request,
                        Authentication auth) {
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(jobWorkflowService.addStep(jobWorkflowId, request, getCompanyId()));
        }
}
