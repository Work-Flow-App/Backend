package com.workflow.controller.workflow;

import com.workflow.dto.workflow.JobWorkflowResponse;
import com.workflow.dto.workflow.JobWorkflowStepResponse;
import com.workflow.dto.workflow.JobWorkflowStepUpdateRequest;
import com.workflow.entity.Company;
import com.workflow.entity.Job;
import com.workflow.entity.User;
import com.workflow.entity.Workflow;
import com.workflow.repository.JobRepository;
import com.workflow.repository.WorkflowRepository;
import com.workflow.service.company.ICompanyService;
import com.workflow.service.workflow.IJobWorkflowService;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/job-workflows")
@RequiredArgsConstructor
public class JobWorkflowController {

        private final IJobWorkflowService jobWorkflowService;
        private final JobRepository jobRepository;
        private final WorkflowRepository workflowRepository;
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

                Job job = jobRepository.findById(jobId)
                                .filter(j -> j.getCompany().getId().equals(companyId))
                                .orElseThrow(() -> new IllegalStateException("Job not found"));

                Workflow workflow = workflowRepository.findById(workflowId)
                                .filter(w -> w.getCompany().getId().equals(companyId))
                                .orElseThrow(() -> new IllegalStateException("Workflow not found"));

                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(jobWorkflowService.startWorkflow(job, workflow));
        }

        @GetMapping("/jobs/{jobId}")
        public ResponseEntity<JobWorkflowResponse> getJobWorkflow(
                        @PathVariable Long jobId,
                        Authentication auth) {
                Long companyId = getCompanyId(auth);

                Job job = jobRepository.findById(jobId)
                                .filter(j -> j.getCompany().getId().equals(companyId))
                                .orElseThrow(() -> new IllegalStateException("Job not found"));

                JobWorkflowResponse response = jobWorkflowService.getJobWorkflow(job);
                return ResponseEntity.ok(response);
        }

        @PutMapping("/{jobId}/steps/{stepId}")
        public JobWorkflowStepResponse updateStep(
                        @PathVariable Long jobId,
                        @PathVariable Long stepId,
                        @RequestBody JobWorkflowStepUpdateRequest request) {
                return jobWorkflowService.updateStep(jobId, stepId, request);
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

        @DeleteMapping("/job/{jobId}")
        public void deleteByJobId(@PathVariable Long jobId) {
                jobWorkflowService.deleteByJobId(jobId);
        }
}
