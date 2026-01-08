package com.workflow.service.workflow;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workflow.common.constant.workflow.WorkflowStepStatus;
import com.workflow.dto.workflow.JobWorkflowResponse;
import com.workflow.dto.workflow.JobWorkflowStepCreateRequest;
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

                List<JobWorkflowStep> steps = jobWorkflowStepRepository
                                .findByJobWorkflowIdOrderByOrderIndexAsc(jobWorkflow.getId());

                if (steps.isEmpty()) {
                        jobWorkflow.setStatus(WorkflowStepStatus.NOT_STARTED);
                        return;
                }

                if (steps.stream().allMatch(s -> s.getStatus() == WorkflowStepStatus.COMPLETED)) {
                        jobWorkflow.setStatus(WorkflowStepStatus.COMPLETED);
                        jobWorkflow.setCompletedAt(LocalDateTime.now());
                        return;
                }

                if (steps.stream().anyMatch(
                                s -> s.getStatus() == WorkflowStepStatus.STARTED
                                                || s.getStatus() == WorkflowStepStatus.ONGOING)) {

                        jobWorkflow.setStatus(WorkflowStepStatus.ONGOING);
                        jobWorkflow.setCompletedAt(null);
                        return;
                }

                jobWorkflow.setStatus(WorkflowStepStatus.NOT_STARTED);
                jobWorkflow.setCompletedAt(null);
        }

        /*
         * =======================
         * REORDER (1-BASED)
         * =======================
         */

        private void reorderStep1Based(JobWorkflowStep step, int requestedIndex) {

                List<JobWorkflowStep> steps = jobWorkflowStepRepository
                                .findByJobWorkflowIdOrderByOrderIndexAsc(
                                                step.getJobWorkflow().getId());

                // Convert 1-based → 0-based
                int newIndex = requestedIndex - 1;

                steps.remove(step);

                // Clamp index
                newIndex = Math.max(0, Math.min(newIndex, steps.size()));
                steps.add(newIndex, step);

                // Reindex back to 1-based
                for (int i = 0; i < steps.size(); i++) {
                        steps.get(i).setOrderIndex(i + 1);
                }

                jobWorkflowStepRepository.saveAll(steps);
        }

        private void normalizeOrderIndexes(Long jobWorkflowId) {

                List<JobWorkflowStep> steps = jobWorkflowStepRepository
                                .findByJobWorkflowIdOrderByOrderIndexAsc(jobWorkflowId);

                for (int i = 0; i < steps.size(); i++) {
                        steps.get(i).setOrderIndex(i + 1);
                }

                jobWorkflowStepRepository.saveAll(steps);
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

                JobWorkflow jw = jobWorkflowRepository.save(
                                JobWorkflow.builder()
                                                .job(job)
                                                .status(WorkflowStepStatus.NOT_STARTED)
                                                .build());

                List<WorkflowStep> templateSteps = workflowStepRepository
                                .findByWorkflowIdOrderByOrderIndexAsc(workflow.getId());

                int index = 1; // ✅ 1-based
                for (WorkflowStep ts : templateSteps) {
                        jobWorkflowStepRepository.save(
                                        JobWorkflowStep.builder()
                                                        .jobWorkflow(jw)
                                                        .name(ts.getName())
                                                        .description(ts.getDescription())
                                                        .orderIndex(index++)
                                                        .status(WorkflowStepStatus.NOT_STARTED)
                                                        .build());
                }

                return buildResponse(jw);
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

                JobWorkflow jw = step.getJobWorkflow();

                if (!jw.getJob().getCompany().getId().equals(companyId)) {
                        throw new IllegalArgumentException("Unauthorized access");
                }

                // 🔹 Name / description
                if (request.getName() != null)
                        step.setName(request.getName());
                if (request.getDescription() != null)
                        step.setDescription(request.getDescription());

                // 🔹 Reorder (1-based)
                if (request.getOrderIndex() != null &&
                                !request.getOrderIndex().equals(step.getOrderIndex())) {

                        reorderStep1Based(step, request.getOrderIndex());
                }

                // 🔹 Status
                if (request.getStatus() != null) {
                        step.setStatus(request.getStatus());

                        if (request.getStatus() == WorkflowStepStatus.STARTED) {
                                step.setStartedAt(LocalDateTime.now());
                        }

                        if (request.getStatus() == WorkflowStepStatus.COMPLETED) {
                                step.setCompletedAt(LocalDateTime.now());
                        }
                }

                // 🔹 Workers
                if (request.getAssignedWorkerIds() != null) {

                        Set<Worker> workers = request.getAssignedWorkerIds().stream()
                                        .map(id -> workerRepository.findById(id)
                                                        .orElseThrow(() -> new IllegalStateException(
                                                                        "Worker not found")))
                                        .peek(w -> {
                                                if (!w.getCompany().getId().equals(companyId)) {
                                                        throw new IllegalArgumentException("Worker not in company");
                                                }
                                        })
                                        .collect(Collectors.toSet());

                        step.getAssignedWorkers().clear();
                        step.getAssignedWorkers().addAll(workers);
                }

                updateJobWorkflowStatus(jw);
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

                JobWorkflow jw = jobWorkflowRepository.findById(jobWorkflowId)
                                .orElseThrow(() -> new IllegalStateException("Job workflow not found"));

                if (!jw.getJob().getCompany().getId().equals(companyId)) {
                        throw new IllegalArgumentException("Unauthorized access");
                }

                List<JobWorkflowStep> existingSteps = jobWorkflowStepRepository
                                .findByJobWorkflowIdOrderByOrderIndexAsc(jw.getId());

                Map<Long, JobWorkflowStep> existingMap = existingSteps.stream()
                                .collect(Collectors.toMap(JobWorkflowStep::getId, s -> s));

                Set<Long> incomingIds = new HashSet<>();

                // CREATE / UPDATE
                for (JobWorkflowStepUpdateRequest sr : request.getSteps()) {

                        if (sr.getId() != null) {

                                JobWorkflowStep step = existingMap.get(sr.getId());
                                if (step == null) {
                                        throw new IllegalStateException("Invalid step id " + sr.getId());
                                }

                                step.setName(sr.getName());
                                step.setDescription(sr.getDescription());
                                step.setOrderIndex(sr.getOrderIndex());
                                incomingIds.add(step.getId());

                        } else {
                                jobWorkflowStepRepository.save(
                                                JobWorkflowStep.builder()
                                                                .jobWorkflow(jw)
                                                                .name(sr.getName())
                                                                .description(sr.getDescription())
                                                                .orderIndex(sr.getOrderIndex())
                                                                .status(WorkflowStepStatus.NOT_STARTED)
                                                                .build());
                        }
                }

                // DELETE removed
                for (JobWorkflowStep step : existingSteps) {
                        if (!incomingIds.contains(step.getId())) {
                                jobWorkflowStepRepository.delete(step);
                        }
                }

                // Normalize order (1-based)
                normalizeOrderIndexes(jw.getId());

                updateJobWorkflowStatus(jw);
                return buildResponse(jw);
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

                List<JobWorkflowStep> steps = jobWorkflowStepRepository.findByJobWorkflowIdOrderByOrderIndexAsc(
                                jobWorkflow.getId());

                for (JobWorkflowStep step : steps) {
                        step.getAssignedWorkers().add(worker);
                }

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

/*         @Transactional
        public JobWorkflowStep addStep(Long jobWorkflowId, JobWorkflowStepCreateRequest req) {

                JobWorkflow jw = jobWorkflowRepository.findById(jobWorkflowId)
                                .orElseThrow();

                return jobWorkflowStepRepository.save(
                                JobWorkflowStep.builder()
                                                .jobWorkflow(jw)
                                                .name(req.getName())
                                                .description(req.getDescription())
                                                .orderIndex(req.getOrderIndex())
                                                .status(WorkflowStepStatus.NOT_STARTED)
                                                .build());
        } */

        /*
         * =======================
         * MAPPERS
         * =======================
         */
        private JobWorkflowStepResponse mapStep(JobWorkflowStep step) {
                return JobWorkflowStepResponse.builder()
                                .id(step.getId())
                                .name(step.getName())
                                .description(step.getDescription())
                                .orderIndex(step.getOrderIndex()) // ✅ 1-based exposed
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
                                .findByJobWorkflowIdOrderByOrderIndexAsc(jw.getId())
                                .stream()
                                .map(this::mapStep)
                                .toList();

                return JobWorkflowResponse.builder()
                                .id(jw.getId())
                                .jobId(jw.getJob().getId())
                                .steps(steps)
                                .status(jw.getStatus())
                                .build();
        }
}
