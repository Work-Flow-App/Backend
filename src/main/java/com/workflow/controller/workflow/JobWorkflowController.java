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

import com.workflow.dto.workflow.JobWorkflowResponse;
import com.workflow.dto.workflow.JobWorkflowStepResponse;
import com.workflow.dto.workflow.JobWorkflowStepUpdateRequest;
import com.workflow.dto.workflow.JobWorkflowUpdateRequest;
import com.workflow.entity.Company;
import com.workflow.entity.Job;
import com.workflow.entity.User;
import com.workflow.entity.Workflow;
import com.workflow.repository.JobRepository;
import com.workflow.repository.WorkflowRepository;
import com.workflow.service.company.ICompanyService;
import com.workflow.service.workflow.IJobWorkflowService;

import lombok.RequiredArgsConstructor;

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
                                .body(jobWorkflowService.startWorkflow(job, workflow, companyId));
        }

        @PutMapping("/{jobWorkflowId}")
        public ResponseEntity<JobWorkflowResponse> updateJobWorkflow(
                        @PathVariable Long jobWorkflowId,
                        @RequestBody JobWorkflowUpdateRequest request,
                        Authentication auth) {

                Long companyId = getCompanyId(auth);
                JobWorkflowResponse response = jobWorkflowService.updateJobWorkflowById(
                                jobWorkflowId, request, companyId);

                return ResponseEntity.ok(response);
        }

        @GetMapping("/jobs/{jobId}")
        public ResponseEntity<JobWorkflowResponse> getJobWorkflow(
                        @PathVariable Long jobId,
                        Authentication auth) {
                Long companyId = getCompanyId(auth);

                Job job = jobRepository.findById(jobId)
                                .filter(j -> j.getCompany().getId().equals(companyId))
                                .orElseThrow(() -> new IllegalStateException("Job not found"));

                JobWorkflowResponse response = jobWorkflowService.getJobWorkflow(job, companyId);
                return ResponseEntity.ok(response);
        }

        @PutMapping("/{jobId}/steps/{stepId}")
        public JobWorkflowStepResponse updateStep(
                        @PathVariable Long jobId,
                        @PathVariable Long stepId,
                        @RequestBody JobWorkflowStepUpdateRequest request,
                        Authentication auth) {

                return jobWorkflowService.updateStep(
                                jobId, stepId, request, getCompanyId(auth));
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
        public void deleteByJobId(
                        @PathVariable Long jobId,
                        Authentication auth) {

                jobWorkflowService.deleteByJobId(jobId, getCompanyId(auth));
        }

        @PutMapping("/{jobWorkflowId}/assign-a-worker/{workerId}")
        public ResponseEntity<JobWorkflowResponse> assignWorkerToAllSteps(
                        @PathVariable Long jobWorkflowId,
                        @PathVariable Long workerId,
                        Authentication auth) {

                Long companyId = getCompanyId(auth);

                // Service now returns JobWorkflowResponse DTO
                JobWorkflowResponse response = jobWorkflowService.assignAWorkerToAllSteps(
                                jobWorkflowId, workerId, companyId);

                return ResponseEntity.ok(response);
        }

}
