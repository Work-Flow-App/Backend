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
import com.workflow.dto.workflow.JobWorkflowUpdateRequest;
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

        /*
         * =======================
         * INTERNAL HELPERS
         * =======================
         */

        private void updateJobWorkflowStatus(JobWorkflow jobWorkflow) {
                List<JobWorkflowStep> steps = jobWorkflowStepRepository.findByJobWorkflowIdOrderByStep_OrderIndexAsc(
                                jobWorkflow.getId());

                if (steps.isEmpty()) {
                        jobWorkflow.setStatus(WorkflowStepStatus.NOT_STARTED);
                        return;
                }

                if (steps.stream().allMatch(s -> s.getStatus() == WorkflowStepStatus.COMPLETED)) {
                        jobWorkflow.setStatus(WorkflowStepStatus.COMPLETED);
                        jobWorkflow.setCompletedAt(LocalDateTime.now());
                } else if (steps.stream().anyMatch(
                                s -> s.getStatus() == WorkflowStepStatus.STARTED
                                                || s.getStatus() == WorkflowStepStatus.ONGOING)) {
                        jobWorkflow.setStatus(WorkflowStepStatus.ONGOING);
                        jobWorkflow.setCompletedAt(null);
                } else {
                        jobWorkflow.setStatus(WorkflowStepStatus.NOT_STARTED);
                        jobWorkflow.setCompletedAt(null);
                }
        }

        /**
         * Workers are DERIVED from step assignments.
         * This is the ONLY place JobWorkflow.workers is mutated.
         */
        private void syncWorkflowWorkers(JobWorkflow jobWorkflow) {

                Set<Worker> workers = jobWorkflowStepRepository
                                .findByJobWorkflowIdOrderByStep_OrderIndexAsc(jobWorkflow.getId())
                                .stream()
                                .flatMap(step -> step.getAssignedWorkers().stream())
                                .collect(Collectors.toSet());

                jobWorkflow.getWorkers().clear();
                jobWorkflow.getWorkers().addAll(workers);
        }

        /*
         * =======================
         * START WORKFLOW
         * =======================
         */

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

                List<WorkflowStep> steps = workflowStepRepository.findByWorkflowIdOrderByOrderIndexAsc(
                                workflow.getId());

                for (WorkflowStep step : steps) {
                        jobWorkflowStepRepository.save(
                                        JobWorkflowStep.builder()
                                                        .jobWorkflow(jobWorkflow)
                                                        .step(step)
                                                        .status(WorkflowStepStatus.INITIATED)
                                                        .build());
                }

                return buildResponse(jobWorkflow);
        }

        /*
         * =======================
         * READ OPERATIONS
         * =======================
         */

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

        @Override
        public JobWorkflowResponse getJobWorkflow(Job job, Long companyId) {

                if (!job.getCompany().getId().equals(companyId)) {
                        throw new IllegalArgumentException("Job does not belong to company");
                }

                JobWorkflow jobWorkflow = jobWorkflowRepository.findByJobId(job.getId())
                                .orElseThrow(
                                                () -> new IllegalStateException("Workflow not started for this job"));

                return buildResponse(jobWorkflow);
        }

        /*
         * =======================
         * UPDATE SINGLE STEP
         * =======================
         */

        @Override
        public JobWorkflowStepResponse updateStep(
                        Long jobId,
                        Long stepId,
                        JobWorkflowStepUpdateRequest request,
                        Long companyId) {

                JobWorkflowStep step = jobWorkflowStepRepository.findById(stepId)
                                .orElseThrow(() -> new IllegalStateException("Step not found"));

                JobWorkflow jobWorkflow = step.getJobWorkflow();

                if (!jobWorkflow.getJob().getCompany().getId().equals(companyId)) {
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

                if (request.getAssignedWorkerIds() != null) {
                        Set<Worker> workers = request.getAssignedWorkerIds().stream()
                                        .map(id -> workerRepository.findById(id)
                                                        .orElseThrow(() -> new IllegalStateException(
                                                                        "Worker not found")))
                                        .peek(w -> {
                                                if (!w.getCompany().getId().equals(companyId)) {
                                                        throw new IllegalArgumentException(
                                                                        "Worker does not belong to company");
                                                }
                                        })
                                        .collect(Collectors.toSet());

                        step.getAssignedWorkers().clear();
                        step.getAssignedWorkers().addAll(workers);
                }

                syncWorkflowWorkers(jobWorkflow);
                updateJobWorkflowStatus(jobWorkflow);

                return mapStep(step);
        }

        /*
         * =======================
         * UPDATE WORKFLOW (BULK)
         * =======================
         */

        @Override
        public JobWorkflowResponse updateJobWorkflowById(
                        Long jobWorkflowId,
                        JobWorkflowUpdateRequest request,
                        Long companyId) {

                JobWorkflow jobWorkflow = jobWorkflowRepository.findById(jobWorkflowId)
                                .orElseThrow(() -> new IllegalStateException("Job workflow not found"));

                if (!jobWorkflow.getJob().getCompany().getId().equals(companyId)) {
                        throw new IllegalArgumentException("Unauthorized access");
                }

                if (request.getSteps() != null) {
                        for (JobWorkflowStepUpdateRequest stepUpdate : request.getSteps()) {
                                JobWorkflowStep step = jobWorkflowStepRepository.findById(stepUpdate.getId())
                                                .orElseThrow(() -> new IllegalStateException("Step not found"));

                                if (!step.getJobWorkflow().getId().equals(jobWorkflowId)) {
                                        throw new IllegalArgumentException("Step does not belong to this workflow");
                                }

                                if (stepUpdate.getStatus() != null) {
                                        step.setStatus(stepUpdate.getStatus());

                                        if (stepUpdate.getStatus() == WorkflowStepStatus.STARTED) {
                                                step.setStartedAt(LocalDateTime.now());
                                        } else if (stepUpdate.getStatus() == WorkflowStepStatus.COMPLETED) {
                                                step.setCompletedAt(LocalDateTime.now());
                                        }
                                }

                                if (stepUpdate.getAssignedWorkerIds() != null) {
                                        Set<Worker> workers = stepUpdate.getAssignedWorkerIds().stream()
                                                        .map(id -> workerRepository.findById(id)
                                                                        .orElseThrow(() -> new IllegalStateException(
                                                                                        "Worker not found")))
                                                        .peek(w -> {
                                                                if (!w.getCompany().getId().equals(companyId)) {
                                                                        throw new IllegalArgumentException(
                                                                                        "Worker does not belong to company");
                                                                }
                                                        })
                                                        .collect(Collectors.toSet());

                                        step.getAssignedWorkers().clear();
                                        step.getAssignedWorkers().addAll(workers);
                                }

                        }
                }

                syncWorkflowWorkers(jobWorkflow);
                updateJobWorkflowStatus(jobWorkflow);

                return buildResponse(jobWorkflow);
        }

        /*
         * =======================
         * ASSIGN WORKER TO ALL STEPS
         * =======================
         */

        @Override
        public JobWorkflowResponse assignAWorkerToAllSteps(
                        Long jobWorkflowId, Long workerId, Long companyId) {

                JobWorkflow jobWorkflow = jobWorkflowRepository.findById(jobWorkflowId)
                                .orElseThrow(() -> new IllegalStateException("Job workflow not found"));

                if (!jobWorkflow.getJob().getCompany().getId().equals(companyId)) {
                        throw new IllegalArgumentException("Unauthorized access");
                }

                Worker worker = workerRepository.findById(workerId)
                                .orElseThrow(() -> new IllegalStateException("Worker not found"));

                if (!worker.getCompany().getId().equals(companyId)) {
                        throw new IllegalArgumentException("Worker does not belong to company");
                }

                List<JobWorkflowStep> steps = jobWorkflowStepRepository.findByJobWorkflowIdOrderByStep_OrderIndexAsc(
                                jobWorkflow.getId());

                for (JobWorkflowStep step : steps) {
                        step.getAssignedWorkers().add(worker);
                }

                syncWorkflowWorkers(jobWorkflow);
                updateJobWorkflowStatus(jobWorkflow);

                return buildResponse(jobWorkflow);
        }

        /*
         * =======================
         * DELETE
         * =======================
         */

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

        /*
         * =======================
         * MAPPERS
         * =======================
         */

        private JobWorkflowStepResponse mapStep(JobWorkflowStep step) {
                return JobWorkflowStepResponse.builder()
                                .id(step.getId())
                                .name(step.getStep().getName())
                                .status(step.getStatus())
                                .startedAt(step.getStartedAt())
                                .completedAt(step.getCompletedAt())
                                .assignedWorkerIds(
                                                step.getAssignedWorkers()
                                                                .stream()
                                                                .map(Worker::getId)
                                                                .collect(Collectors.toSet()))
                                .build();
        }

        private JobWorkflowResponse buildResponse(JobWorkflow jw) {

                List<JobWorkflowStepResponse> steps = jobWorkflowStepRepository
                                .findByJobWorkflowIdOrderByStep_OrderIndexAsc(
                                                jw.getId())
                                .stream()
                                .map(this::mapStep)
                                .toList();

                Set<Long> workerIds = jw.getWorkers().stream().map(Worker::getId).collect(Collectors.toSet());

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
