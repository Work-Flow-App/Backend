package com.workflow.service.workflow;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

        private void updateJobWorkflowStatus(JobWorkflow jobWorkflow) {
                List<JobWorkflowStep> steps = jobWorkflowStepRepository
                                .findByJobWorkflowIdOrderByStep_OrderIndexAsc(jobWorkflow.getId());

                if (steps.stream().allMatch(s -> s.getStatus() == WorkflowStepStatus.COMPLETED)) {
                        jobWorkflow.setStatus(WorkflowStepStatus.COMPLETED);
                        jobWorkflow.setCompletedAt(LocalDateTime.now());
                } else if (steps.stream().anyMatch(s -> s.getStatus() == WorkflowStepStatus.STARTED
                                || s.getStatus() == WorkflowStepStatus.ONGOING)) {
                        jobWorkflow.setStatus(WorkflowStepStatus.ONGOING);
                } else {
                        jobWorkflow.setStatus(WorkflowStepStatus.NOT_STARTED);
                }
        }

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
                                                .status(WorkflowStepStatus.INITIATED)
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
        }

        @Override
        public JobWorkflowStepResponse updateStep(
                        Long jobId,
                        Long stepId,
                        JobWorkflowStepUpdateRequest request,
                        Long companyId) {

                JobWorkflowStep step = jobWorkflowStepRepository.findById(stepId)
                                .orElseThrow(() -> new IllegalStateException("Step not found"));

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
                        step.getJobWorkflow().getWorkers().add(worker);
                }

                updateJobWorkflowStatus(step.getJobWorkflow());

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

        @Transactional
        public JobWorkflowResponse assignAWorkerToAllSteps(Long jobWorkflowId, Long workerId, Long companyId) {

                // 1️⃣ Load JobWorkflow
                JobWorkflow jobWorkflow = jobWorkflowRepository.findById(jobWorkflowId)
                                .orElseThrow(() -> new IllegalStateException("Job workflow not found"));

                if (!jobWorkflow.getJob().getCompany().getId().equals(companyId)) {
                        throw new IllegalArgumentException("Unauthorized access");
                }

                // 2️⃣ Load Worker
                Worker worker = workerRepository.findById(workerId)
                                .orElseThrow(() -> new IllegalStateException("Worker not found"));

                if (!worker.getCompany().getId().equals(companyId)) {
                        throw new IllegalArgumentException("Worker does not belong to company");
                }

                // 3️⃣ Assign worker to all steps
                List<JobWorkflowStep> steps = jobWorkflowStepRepository
                                .findByJobWorkflowIdOrderByStep_OrderIndexAsc(jobWorkflow.getId());

                for (JobWorkflowStep step : steps) {
                        step.setAssignedWorker(worker);
                }

                // 4️⃣ Add worker to JobWorkflow workers set
                jobWorkflow.getWorkers().add(worker);

                // 5️⃣ Update JobWorkflow status
                updateJobWorkflowStatus(jobWorkflow);

                // 6️⃣ Save and return mapped DTO
                jobWorkflowRepository.save(jobWorkflow);
                return buildResponse(jobWorkflow);
        }

        private JobWorkflowResponse buildResponse(JobWorkflow jw) {
                List<JobWorkflowStepResponse> steps = jobWorkflowStepRepository
                                .findByJobWorkflowIdOrderByStep_OrderIndexAsc(jw.getId())
                                .stream()
                                .map(this::mapStep)
                                .toList();

                Set<Long> workerIds = jw.getWorkers().stream()
                                .map(Worker::getId)
                                .collect(Collectors.toSet());

                return JobWorkflowResponse.builder()
                                .id(jw.getId())
                                .jobId(jw.getJob().getId())
                                .workflowId(jw.getWorkflow().getId())
                                .steps(steps)
                                .status(jw.getStatus())
                                .workerIds(workerIds)
                                .build();
        }

}
