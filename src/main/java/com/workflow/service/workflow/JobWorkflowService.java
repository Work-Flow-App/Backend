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

        @Override
        public JobWorkflowResponse startWorkflow(Job job, Workflow workflow, Long companyId) {

                if (!job.getCompany().getId().equals(companyId)) {
                        throw new IllegalArgumentException("Job does not belong to company");
                }

                if (!workflow.getCompany().getId().equals(companyId)) {
                        throw new IllegalArgumentException("Workflow does not belong to company");
                }

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
                                                        .status(WorkflowStepStatus.INITIATED)
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

        public JobWorkflowResponse getJobWorkflow(Job job, Long companyId) {

                if (!job.getCompany().getId().equals(companyId)) {
                        throw new IllegalArgumentException("Job does not belong to company");
                }

                JobWorkflow jobWorkflow = jobWorkflowRepository.findByJobId(job.getId())
                                .orElseThrow(() -> new IllegalStateException("Workflow not started for this job"));

                return buildResponse(jobWorkflow);

                /*
                 * List<JobWorkflowStepResponse> steps = jobWorkflowStepRepository
                 * .findByJobWorkflowIdOrderByStep_OrderIndexAsc(jobWorkflow.getId())
                 * .stream()
                 * .<JobWorkflowStepResponse>map(s -> JobWorkflowStepResponse.builder()
                 * .id(s.getId())
                 * .name(s.getStep().getName())
                 * .status(s.getStatus())
                 * .startedAt(s.getStartedAt())
                 * .completedAt(s.getCompletedAt())
                 * .assignedWorkerId(
                 * s.getAssignedWorker() != null
                 * ? s.getAssignedWorker().getId()
                 * : null)
                 * .build())
                 * .toList();
                 * 
                 * return JobWorkflowResponse.builder()
                 * .jobId(job.getId())
                 * .workflowId(jobWorkflow.getWorkflow().getId())
                 * .steps(steps)
                 * .build();
                 */
        }

        @Override
        public JobWorkflowStepResponse updateStep(
                        Long jobId,
                        Long stepId,
                        JobWorkflowStepUpdateRequest request,
                        Long companyId) {

                JobWorkflowStep step = jobWorkflowStepRepository.findById(stepId)
                                .orElseThrow(() -> new IllegalStateException("Step not found"));

                Job job = step.getJobWorkflow().getJob();

                if (!job.getId().equals(jobId)) {
                        throw new IllegalArgumentException("Step does not belong to job");
                }

                if (!job.getCompany().getId().equals(companyId)) {
                        throw new IllegalArgumentException("Unauthorized access");
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

                        if (!worker.getCompany().getId().equals(companyId)) {
                                throw new IllegalArgumentException("Worker does not belong to company");
                        }

                        step.setAssignedWorker(worker);
                }

                return mapStep(step);
        }

        private JobWorkflowStepResponse mapStep(JobWorkflowStep step) {
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

        @Override
        public void deleteByJobId(Long jobId, Long companyId) {

                JobWorkflow jobWorkflow = jobWorkflowRepository.findByJobId(jobId)
                                .orElseThrow(() -> new IllegalStateException("Job workflow not found"));

                if (!jobWorkflow.getJob().getCompany().getId().equals(companyId)) {
                        throw new IllegalArgumentException("Unauthorized delete");
                }

                jobWorkflowStepRepository.deleteByJobWorkflowId(jobWorkflow.getId());
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
