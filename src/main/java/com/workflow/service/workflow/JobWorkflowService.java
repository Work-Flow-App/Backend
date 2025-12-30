package com.workflow.service.workflow;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workflow.common.constant.workflow.WorkflowStepStatus;
import com.workflow.dto.workflow.JobWorkflowResponse;
import com.workflow.dto.workflow.JobWorkflowStepResponse;
import com.workflow.dto.workflow.JobWorkflowStepUpdateRequest;
import com.workflow.entity.Job;
import com.workflow.entity.JobWorkflow;
import com.workflow.entity.JobWorkflowStep;
import com.workflow.entity.Worker;
import com.workflow.entity.Workflow;
import com.workflow.entity.WorkflowStep;
import com.workflow.repository.JobWorkflowRepository;
import com.workflow.repository.JobWorkflowStepRepository;
import com.workflow.repository.WorkerRepository;
import com.workflow.repository.WorkflowStepRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class JobWorkflowService implements IJobWorkflowService {

        private final JobWorkflowRepository jobWorkflowRepository;
        private final JobWorkflowStepRepository jobWorkflowStepRepository;
        private final WorkflowStepRepository workflowStepRepository;
        private final WorkerRepository workerRepository;

        public JobWorkflowResponse startWorkflow(Job job, Workflow workflow) {

                JobWorkflow jobWorkflow = jobWorkflowRepository.save(
                                JobWorkflow.builder()
                                                .job(job)
                                                .workflow(workflow)
                                                .build());

                List<WorkflowStep> steps = workflowStepRepository
                                .findByWorkflowIdOrderByOrderIndexAsc(workflow.getId());

                for (WorkflowStep s : steps) {
                        jobWorkflowStepRepository.save(
                                        JobWorkflowStep.builder()
                                                        .jobWorkflow(jobWorkflow)
                                                        .step(s)
                                                        .status(WorkflowStepStatus.INITIATED) // set all steps as
                                                                                              // INITIATED
                                                                                              // initially
                                                        .build());
                }
                return buildResponse(jobWorkflow);
        }

        @Override
        public JobWorkflowResponse getJobWorkflowById(Long jobWorkflowId, Long companyId) {

                JobWorkflow jobWorkflow = jobWorkflowRepository.findById(jobWorkflowId)
                                .filter(jw -> jw.getJob().getCompany().getId().equals(companyId))
                                .orElseThrow(() -> new IllegalStateException("Job workflow not found"));

                return buildResponse(jobWorkflow);
        }

        @Override
        public List<JobWorkflowResponse> getAllJobWorkflows(Long companyId) {

                return jobWorkflowRepository.findByJob_Company_Id(companyId)
                                .stream()
                                .map(this::buildResponse)
                                .toList();
        }

        public JobWorkflowResponse getJobWorkflow(Job job) {
                JobWorkflow jobWorkflow = jobWorkflowRepository.findByJobId(job.getId())
                                .orElseThrow(() -> new IllegalStateException("Workflow not started for this job"));

                List<JobWorkflowStepResponse> steps = jobWorkflowStepRepository
                                .findByJobWorkflowIdOrderByStep_OrderIndexAsc(jobWorkflow.getId())
                                .stream()
                                .<JobWorkflowStepResponse>map(s -> JobWorkflowStepResponse.builder()
                                                .id(s.getId())
                                                .name(s.getStep().getName())
                                                .status(s.getStatus())
                                                .startedAt(s.getStartedAt())
                                                .completedAt(s.getCompletedAt())
                                                .assignedWorkerId(
                                                                s.getAssignedWorker() != null
                                                                                ? s.getAssignedWorker().getId()
                                                                                : null)
                                                .build())
                                .toList();

                return JobWorkflowResponse.builder()
                                .jobId(job.getId())
                                .workflowId(jobWorkflow.getWorkflow().getId())
                                .steps(steps)
                                .build();
        }

        @Transactional
        public JobWorkflowStepResponse updateStep(Long jobId, Long stepId, JobWorkflowStepUpdateRequest request) {
                JobWorkflowStep step = jobWorkflowStepRepository.findById(stepId)
                                .orElseThrow(() -> new IllegalStateException("Step not found"));

                if (!step.getJobWorkflow().getJob().getId().equals(jobId)) {
                        throw new IllegalArgumentException("Step does not belong to this job");
                }

                if (request.getStatus() != null) {
                        step.setStatus(request.getStatus());
                        if (request.getStatus() == WorkflowStepStatus.STARTED) {
                                step.setStartedAt(LocalDateTime.now());
                        } else if (request.getStatus() == WorkflowStepStatus.COMPLETED) {
                                step.setCompletedAt(LocalDateTime.now());
                        }
                }

                if (request.getAssignedWorkerId() != null) {
                        Worker worker = workerRepository.findById(request.getAssignedWorkerId())
                                        .orElseThrow(() -> new IllegalStateException("Worker not found"));
                        step.setAssignedWorker(worker);
                }

                return JobWorkflowStepResponse.builder()
                                .id(step.getId())
                                .name(step.getStep().getName())
                                .status(step.getStatus())
                                .startedAt(step.getStartedAt())
                                .completedAt(step.getCompletedAt())
                                .assignedWorkerId(
                                                step.getAssignedWorker() != null
                                                                ? step.getAssignedWorker().getId()
                                                                : null)
                                .build();
        }

        @Transactional
        public void deleteByJobId(Long jobId) {

                JobWorkflow jobWorkflow = jobWorkflowRepository.findByJobId(jobId)
                                .orElseThrow(() -> new IllegalStateException("Job workflow not found for job"));

                // delete steps first (important!)
                jobWorkflowStepRepository.deleteByJobWorkflowId(jobWorkflow.getId());

                // delete workflow
                jobWorkflowRepository.delete(jobWorkflow);
        }

        private JobWorkflowResponse buildResponse(JobWorkflow jw) {

                List<JobWorkflowStepResponse> steps = jobWorkflowStepRepository
                                .findByJobWorkflowIdOrderByStep_OrderIndexAsc(jw.getId())
                                .stream()
                                .map(s -> JobWorkflowStepResponse.builder()
                                                .id(s.getId())
                                                .name(s.getStep().getName())
                                                .status(s.getStatus())
                                                .startedAt(s.getStartedAt())
                                                .completedAt(s.getCompletedAt())
                                                .assignedWorkerId(
                                                                s.getAssignedWorker() != null
                                                                                ? s.getAssignedWorker().getId()
                                                                                : null)
                                                .build())
                                .toList();

                return JobWorkflowResponse.builder()
                                .id(jw.getId())
                                .jobId(jw.getJob().getId())
                                .workflowId(jw.getWorkflow().getId())
                                .steps(steps)
                                .build();
        }

}
