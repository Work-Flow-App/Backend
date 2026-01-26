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

import com.workflow.dto.workflow.JobWorkflowResponse;
import com.workflow.dto.workflow.JobWorkflowStepResponse;
import com.workflow.dto.workflow.JobWorkflowStepUpdateRequest;
import com.workflow.dto.workflow.JobWorkflowUpdateRequest;
import com.workflow.entity.Company;
import com.workflow.entity.User;
import com.workflow.service.company.ICompanyService;
import com.workflow.service.workflow.IJobWorkflowService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Job Workflows")
@RestController
@RequestMapping("/api/v1/job-workflows")
@RequiredArgsConstructor
public class JobWorkflowController {

        private final IJobWorkflowService jobWorkflowService;
        private final ICompanyService companyService;

        private Long getCompanyId(Authentication auth) {
                User user = (User) auth.getPrincipal();
                Company company = companyService.findCompanyByUserId(user.getId());
                return company.getId();
        }

        @PostMapping("/jobs/{jobId}/workflows/{workflowId}/start")
        public ResponseEntity<JobWorkflowResponse> startWorkflow(
                        @PathVariable Long jobId,
                        @PathVariable Long workflowId,
                        Authentication auth) {
                Long companyId = getCompanyId(auth);
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(jobWorkflowService.startWorkflowForJob(jobId, workflowId, companyId));
        }

        @PutMapping("/{jobWorkflowId}")
        public ResponseEntity<JobWorkflowResponse> updateJobWorkflow(
                        @PathVariable Long jobWorkflowId,
                        @Valid @RequestBody JobWorkflowUpdateRequest request,
                        Authentication auth) {
                return ResponseEntity.ok(jobWorkflowService.updateJobWorkflowById(
                                jobWorkflowId, request, getCompanyId(auth)));
        }

        @GetMapping("/jobs/{jobId}")
        public ResponseEntity<JobWorkflowResponse> getJobWorkflow(
                        @PathVariable Long jobId,
                        Authentication auth) {
                Long companyId = getCompanyId(auth);
                return ResponseEntity.ok(jobWorkflowService.getJobWorkflowByJobId(jobId, companyId));
        }

        @PutMapping("/{jobWorkflowId}/steps/{stepId}")
        public ResponseEntity<JobWorkflowStepResponse> updateStep(
                        @PathVariable Long jobWorkflowId,
                        @PathVariable Long stepId,
                        @Valid @RequestBody JobWorkflowStepUpdateRequest request,
                        Authentication auth) {
                return ResponseEntity.ok(jobWorkflowService.updateStep(
                                jobWorkflowId, stepId, request, getCompanyId(auth)));
        }

        @GetMapping("/{jobWorkflowId}")
        public ResponseEntity<JobWorkflowResponse> getJobWorkflowById(
                        @PathVariable Long jobWorkflowId,
                        Authentication auth) {

                Long companyId = getCompanyId(auth);
                return ResponseEntity.ok(
                                jobWorkflowService.getJobWorkflowById(jobWorkflowId, companyId));
        }

        @GetMapping
        public ResponseEntity<List<JobWorkflowResponse>> getAllJobWorkflows(Authentication auth) {
                Long companyId = getCompanyId(auth);
                return ResponseEntity.ok(jobWorkflowService.getAllJobWorkflows(companyId));
        }

        @DeleteMapping("/jobs/{jobId}")
        public ResponseEntity<Void> deleteByJobId(
                        @PathVariable Long jobId,
                        Authentication auth) {
                jobWorkflowService.deleteByJobId(jobId, getCompanyId(auth));
                return ResponseEntity.noContent().build();
        }

        @PutMapping("/{jobWorkflowId}/assign-worker/{workerId}")
        public ResponseEntity<JobWorkflowResponse> assignWorkerToAllSteps(
                        @PathVariable Long jobWorkflowId,
                        @PathVariable Long workerId,
                        Authentication auth) {
                return ResponseEntity.ok(jobWorkflowService.assignAWorkerToAllSteps(
                                jobWorkflowId, workerId, getCompanyId(auth)));
        }

}
