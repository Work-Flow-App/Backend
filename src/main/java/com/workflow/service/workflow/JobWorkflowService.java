package com.workflow.service.workflow;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workflow.common.constant.workflow.JobWorkflowStepActivityType;
import com.workflow.common.constant.workflow.WorkflowStepStatus;
import com.workflow.common.exception.business.JobNotFoundException;
import com.workflow.common.exception.business.JobWorkflowNotFoundException;
import com.workflow.common.exception.business.JobWorkflowStepNotFoundException;
import com.workflow.common.exception.business.UnauthorizedWorkflowAccessException;
import com.workflow.common.exception.business.WorkerNotFoundException;
import com.workflow.common.exception.business.WorkflowAlreadyStartedException;
import com.workflow.common.exception.business.WorkflowNotFoundException;
import com.workflow.common.exception.business.WorkflowNotStartedException;
import com.workflow.dto.workflow.JobWorkflowResponse;
import com.workflow.dto.workflow.JobWorkflowStepCreateRequest;
import com.workflow.dto.workflow.JobWorkflowStepResponse;
import com.workflow.dto.workflow.JobWorkflowStepUpdateRequest;
import com.workflow.dto.workflow.JobWorkflowUpdateRequest;
import com.workflow.entity.job.Job;
import com.workflow.entity.job.JobWorkflow;
import com.workflow.entity.job.JobWorkflowStep;
import com.workflow.entity.auth.User;
import com.workflow.entity.worker.Worker;
import com.workflow.entity.workflow.Workflow;
import com.workflow.entity.workflow.WorkflowStep;
import com.workflow.repository.job.JobRepository;
import com.workflow.repository.job.JobWorkflowRepository;
import com.workflow.repository.job.JobWorkflowStepRepository;
import com.workflow.repository.worker.WorkerRepository;
import com.workflow.repository.workflow.WorkflowRepository;
import com.workflow.repository.workflow.WorkflowStepRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class JobWorkflowService implements IJobWorkflowService {
        private final JobWorkflowRepository jobWorkflowRepository;
        private final JobWorkflowStepRepository jobWorkflowStepRepository;
        private final WorkflowStepRepository workflowStepRepository;
        private final WorkerRepository workerRepository;
        private final JobRepository jobRepository;
        private final WorkflowRepository workflowRepository;
        private final IStepActivityService stepActivityService;
        private final JobWorkflowMapper jobWorkflowMapper;

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

        private void logStep(
                        JobWorkflowStep step,
                        User actor,
                        JobWorkflowStepActivityType type,
                        String message) {

                stepActivityService.log(step, actor, type, message);
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

                int oldIndex = step.getOrderIndex();

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
                logStep(
                                step,
                                step.getJobWorkflow().getJob().getCompany().getUser(),
                                JobWorkflowStepActivityType.STEP_REORDERED,
                                "Reordered step from position " + oldIndex + " to " + requestedIndex);
        }

        /**
         * ⚠️ DANGER — Hibernate flush-order / deleted-entity reference risk
         *
         * This method is SAFE *only because* it:
         * - Reloads steps from the database
         * - Operates exclusively on MANAGED, NON-DELETED JobWorkflowStep entities
         * - Logs activity BEFORE any delete occurs
         *
         * ❌ DO NOT:
         * - Call this method after deleting a JobWorkflowStep and before flush
         * - Log activity for a step that has been marked REMOVED
         * - Pass or reuse a collection that may contain deleted steps
         *
         * ❗ Hibernate will throw ObjectDeletedException / TransientObjectException
         * if a new entity (StepActivity) references a deleted JobWorkflowStep
         * in the same persistence context.
         *
         * Rule of thumb:
         * NEVER persist/log a new entity that references an entity
         * scheduled for deletion in the same transaction.
         */

        private void normalizeOrderIndexes(Long jobWorkflowId) {

                List<JobWorkflowStep> steps = jobWorkflowStepRepository
                                .findByJobWorkflowIdOrderByOrderIndexAsc(jobWorkflowId);

                for (int i = 0; i < steps.size(); i++) {
                        JobWorkflowStep step = steps.get(i);
                        int newIndex = i + 1;
                        if (!Integer.valueOf(newIndex).equals(step.getOrderIndex())) {
                                step.setOrderIndex(newIndex);
                        }
                }

                jobWorkflowStepRepository.saveAll(steps);
        }
        /*
         * =======================
         * START WORKFLOW
         * =======================
         */

        @Override
        @Transactional
        public JobWorkflowResponse startWorkflow(Job job, Workflow workflow, Long companyId) {

                if (!job.getCompany().getId().equals(companyId)) {
                        throw new UnauthorizedWorkflowAccessException("Job does not belong to company");
                }
                if (!workflow.getCompany().getId().equals(companyId)) {
                        throw new UnauthorizedWorkflowAccessException(
                                        "Workflow does not belong to company");
                }
                if (jobWorkflowRepository.findByJobId(job.getId()).isPresent()) {
                        throw new WorkflowAlreadyStartedException(
                                        "Workflow already started for job ID " + job.getId());
                }

                JobWorkflow jw = jobWorkflowRepository.save(
                                JobWorkflow.builder()
                                                .job(job)
                                                .status(WorkflowStepStatus.NOT_STARTED)
                                                .build());
                List<WorkflowStep> templateSteps = workflowStepRepository
                                .findByWorkflowIdOrderByOrderIndexAsc(workflow.getId());

                // Build all steps first without saving individually
                List<JobWorkflowStep> newSteps = new ArrayList<>();
                int index = 1; // ✅ 1-based
                for (WorkflowStep ts : templateSteps) {
                        newSteps.add(JobWorkflowStep.builder()
                                        .jobWorkflow(jw)
                                        .name(ts.getName())
                                        .description(ts.getDescription())
                                        .orderIndex(index++)
                                        .status(WorkflowStepStatus.NOT_STARTED)
                                        .build());
                }

                // Single batch insert for all steps
                List<JobWorkflowStep> savedSteps = jobWorkflowStepRepository.saveAll(newSteps);

                // Batch log activities for all saved steps in one saveAll call
                User actor = job.getCompany().getUser();
                stepActivityService.logAll(savedSteps, actor, JobWorkflowStepActivityType.STEP_CREATED,
                                "Created workflow step");

                return buildResponse(jw);
        }

        @Override
        @Transactional
        public JobWorkflowResponse startWorkflowForJob(Long jobId, Long workflowId, Long companyId) {
                Job job = jobRepository.findById(jobId)
                                .filter(j -> j.getCompany().getId().equals(companyId))
                                .orElseThrow(() -> new JobNotFoundException("Job not found"));

                Workflow workflow = workflowRepository.findById(workflowId)
                                .filter(w -> w.getCompany().getId().equals(companyId))
                                .orElseThrow(() -> new WorkflowNotFoundException("Workflow not found"));

                return startWorkflow(job, workflow, companyId);
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
                                .orElseThrow(() -> new JobWorkflowNotFoundException("Job workflow not found"));
                return buildResponse(jobWorkflow);
        }

        @Override
        public JobWorkflowResponse getJobWorkflowByJobId(Long jobId, Long companyId) {
                Job job = jobRepository.findById(jobId)
                                .filter(j -> j.getCompany().getId().equals(companyId))
                                .orElseThrow(() -> new JobNotFoundException("Job not found"));

                JobWorkflow jobWorkflow = jobWorkflowRepository.findByJobId(job.getId())
                                .orElseThrow(() -> new WorkflowNotStartedException(
                                                "Workflow not started for this job"));

                return buildResponse(jobWorkflow);
        }

        @Override
        @Transactional(readOnly = true)
        public List<JobWorkflowResponse> getAllJobWorkflows(Long companyId) {
                List<JobWorkflow> workflows = jobWorkflowRepository.findByJob_Company_Id(companyId);
                if (workflows.isEmpty())
                        return new java.util.ArrayList<>();

                // Batch-load all steps for all workflows in one query, then group by workflowId
                List<Long> workflowIds = workflows.stream()
                                .map(JobWorkflow::getId)
                                .collect(Collectors.toList());
                Map<Long, List<JobWorkflowStep>> stepsByWorkflowId = jobWorkflowStepRepository
                                .findByJobWorkflowIdInOrderByOrderIndexAsc(workflowIds)
                                .stream()
                                .collect(Collectors.groupingBy(s -> s.getJobWorkflow().getId()));

                return workflows.stream()
                                .map(jw -> buildResponse(jw,
                                                stepsByWorkflowId.getOrDefault(jw.getId(),
                                                                new java.util.ArrayList<>())))
                                .toList();
        }

        @Override
        public JobWorkflowResponse getJobWorkflow(Job job, Long companyId) {
                if (!job.getCompany().getId().equals(companyId)) {
                        throw new UnauthorizedWorkflowAccessException("Job does not belong to company");
                }
                JobWorkflow jobWorkflow = jobWorkflowRepository.findByJobId(job.getId())
                                .orElseThrow(
                                                () -> new WorkflowNotStartedException(
                                                                "Workflow not started for this job"));
                return buildResponse(jobWorkflow);
        }
        /*
         * =======================
         * UPDATE SINGLE STEP
         * =======================
         */

        @Override
        @Transactional
        public JobWorkflowStepResponse updateStep(
                        Long jobWorkflowId,
                        Long stepId,
                        JobWorkflowStepUpdateRequest request,
                        Long companyId) {
                JobWorkflowStep step = jobWorkflowStepRepository.findById(stepId)
                                .orElseThrow(() -> new JobWorkflowStepNotFoundException("Workflow step not found"));

                JobWorkflow jw = step.getJobWorkflow();

                if (!jw.getId().equals(jobWorkflowId)) {
                        throw new JobWorkflowStepNotFoundException("Step does not belong to this workflow");
                }

                if (!jw.getJob().getCompany().getId().equals(companyId)) {
                        throw new UnauthorizedWorkflowAccessException("Job does not belong to company");
                }

                User actor = jw.getJob().getCompany().getUser();

                // 🔹 Name
                if (request.getName() != null) {
                        step.setName(request.getName());
                        logStep(step, actor, JobWorkflowStepActivityType.STEP_UPDATED, "Updated step name");

                }

                // 🔹 Description
                if (request.getDescription() != null) {
                        step.setDescription(request.getDescription());
                        logStep(step, actor, JobWorkflowStepActivityType.STEP_UPDATED, "Updated step description");

                }

                if (request.getOrderIndex() != null &&
                                !request.getOrderIndex().equals(step.getOrderIndex())) {
                        reorderStep1Based(step, request.getOrderIndex());
                }

                if (request.getStatus() != null &&
                                request.getStatus() != step.getStatus()) {

                        WorkflowStepStatus oldStatus = step.getStatus();
                        step.setStatus(request.getStatus());

                        if (request.getStatus() == WorkflowStepStatus.STARTED) {
                                step.setStartedAt(LocalDateTime.now());
                        }
                        if (request.getStatus() == WorkflowStepStatus.COMPLETED) {
                                step.setCompletedAt(LocalDateTime.now());
                        }

                        logStep(
                                        step,
                                        actor,
                                        JobWorkflowStepActivityType.STATUS_CHANGED,
                                        "Status changed from " + oldStatus + " to " + request.getStatus());
                }

                if (request.getAssignedWorkerIds() != null) {

                        Set<Long> previous = step.getAssignedWorkers().stream().map(Worker::getId)
                                        .collect(Collectors.toSet());

                        List<Worker> workerList = workerRepository.findAllById(request.getAssignedWorkerIds());
                        if (workerList.size() != request.getAssignedWorkerIds().size()) {
                                throw new WorkerNotFoundException("One or more workers not found");
                        }
                        for (Worker w : workerList) {
                                if (!w.getCompany().getId().equals(companyId)) {
                                        throw new UnauthorizedWorkflowAccessException(
                                                        "Worker does not belong to company");
                                }
                        }
                        Set<Worker> workers = new HashSet<>(workerList);

                        step.getAssignedWorkers().clear();
                        step.getAssignedWorkers().addAll(workers);

                        for (Worker w : workers) {
                                if (!previous.contains(w.getId())) {
                                        logStep(
                                                        step,
                                                        actor,
                                                        JobWorkflowStepActivityType.WORKER_ASSIGNED,
                                                        "Assigned worker ID " + w.getId());
                                }
                        }

                        for (Long oldId : previous) {
                                if (workers.stream().noneMatch(w -> w.getId().equals(oldId))) {
                                        logStep(
                                                        step,
                                                        actor,
                                                        JobWorkflowStepActivityType.WORKER_REMOVED,
                                                        "Removed worker ID " + oldId);
                                }
                        }
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
        @Transactional
        public JobWorkflowResponse updateJobWorkflowById(
                        Long jobWorkflowId,
                        JobWorkflowUpdateRequest request,
                        Long companyId) {
                JobWorkflow jw = jobWorkflowRepository.findById(jobWorkflowId)
                                .orElseThrow(() -> new JobWorkflowNotFoundException("Job workflow not found"));

                if (!jw.getJob().getCompany().getId().equals(companyId)) {
                        throw new UnauthorizedWorkflowAccessException("Unauthorized access");
                }

                User actor = jw.getJob().getCompany().getUser();

                List<JobWorkflowStep> existingSteps = jobWorkflowStepRepository
                                .findByJobWorkflowIdOrderByOrderIndexAsc(jw.getId());

                Map<Long, JobWorkflowStep> existingMap = existingSteps.stream()
                                .collect(Collectors.toMap(JobWorkflowStep::getId, s -> s));

                Set<Long> incomingIds = new HashSet<>();

                /*
                 * ============================
                 * CREATE / UPDATE STEPS
                 * ============================
                 */
                if (request.getSteps() != null) {
                        for (JobWorkflowStepUpdateRequest sr : request.getSteps()) {

                                WorkflowStepStatus status = sr.getStatus() != null ? sr.getStatus()
                                                : WorkflowStepStatus.NOT_STARTED;
                                LocalDateTime startedAt = null;
                                LocalDateTime completedAt = null;
                                if (status == WorkflowStepStatus.STARTED) {
                                        startedAt = LocalDateTime.now();
                                }
                                if (status == WorkflowStepStatus.COMPLETED) {
                                        completedAt = LocalDateTime.now();
                                }

                                /*
                                 * ============================
                                 * UPDATE EXISTING STEP
                                 * ============================
                                 */
                                if (sr.getId() != null) {

                                        JobWorkflowStep step = existingMap.get(sr.getId());
                                        if (step == null) {
                                                throw new JobWorkflowStepNotFoundException(
                                                                "Invalid workflow step id: " + sr.getId());
                                        }

                                        if (sr.getName() != null) {
                                                step.setName(sr.getName());
                                                stepActivityService.log(
                                                                step,
                                                                actor,
                                                                JobWorkflowStepActivityType.STEP_UPDATED,
                                                                "Updated step name");
                                        }

                                        if (sr.getDescription() != null) {
                                                step.setDescription(sr.getDescription());
                                                stepActivityService.log(
                                                                step,
                                                                actor,
                                                                JobWorkflowStepActivityType.STEP_UPDATED,
                                                                "Updated step description");
                                        }

                                        // 🔹 Order
                                        if (sr.getOrderIndex() != null &&
                                                        !sr.getOrderIndex().equals(step.getOrderIndex())) {

                                                int oldIndex = step.getOrderIndex();
                                                step.setOrderIndex(sr.getOrderIndex());

                                                stepActivityService.log(
                                                                step,
                                                                actor,
                                                                JobWorkflowStepActivityType.STEP_REORDERED,
                                                                "Reordered step from " + oldIndex + " to "
                                                                                + sr.getOrderIndex());
                                        }

                                        // 🔹 Status
                                        if (sr.getStatus() != null &&
                                                        sr.getStatus() != step.getStatus()) {

                                                WorkflowStepStatus oldStatus = step.getStatus();
                                                LocalDateTime oldStartedAt = step.getStartedAt();
                                                LocalDateTime oldCompletedAt = step.getCompletedAt();

                                                step.setStatus(status);
                                                step.setStartedAt(startedAt);
                                                step.setCompletedAt(completedAt);

                                                stepActivityService.log(
                                                                step,
                                                                actor,
                                                                JobWorkflowStepActivityType.STATUS_CHANGED,
                                                                "Status changed from " + oldStatus + " to " + status);
                                                if (oldStartedAt == null && startedAt != null) {
                                                        stepActivityService.log(
                                                                        step,
                                                                        actor,
                                                                        JobWorkflowStepActivityType.STEP_DATA_UPDATED,
                                                                        "Step started");
                                                }

                                                if (oldCompletedAt == null && completedAt != null) {
                                                        stepActivityService.log(
                                                                        step,
                                                                        actor,
                                                                        JobWorkflowStepActivityType.STEP_DATA_UPDATED,
                                                                        "Step completed");
                                                }
                                        }

                                        // 🔹 Workers
                                        if (sr.getAssignedWorkerIds() != null) {

                                                Set<Long> previous = step.getAssignedWorkers().stream()
                                                                .map(Worker::getId)
                                                                .collect(Collectors.toSet());

                                                List<Worker> workerList = workerRepository
                                                                .findAllById(sr.getAssignedWorkerIds());
                                                if (workerList.size() != sr.getAssignedWorkerIds().size()) {
                                                        throw new WorkerNotFoundException(
                                                                        "One or more workers not found");
                                                }
                                                for (Worker w : workerList) {
                                                        if (!w.getCompany().getId().equals(companyId)) {
                                                                throw new UnauthorizedWorkflowAccessException(
                                                                                "Worker does not belong to company");
                                                        }
                                                }
                                                Set<Worker> workers = new HashSet<>(workerList);

                                                step.getAssignedWorkers().clear();
                                                step.getAssignedWorkers().addAll(workers);

                                                for (Worker w : workers) {
                                                        if (!previous.contains(w.getId())) {
                                                                stepActivityService.log(
                                                                                step,
                                                                                actor,
                                                                                JobWorkflowStepActivityType.WORKER_ASSIGNED,
                                                                                "Assigned worker ID " + w.getId());
                                                        }
                                                }

                                                for (Long oldId : previous) {
                                                        if (workers.stream().noneMatch(w -> w.getId().equals(oldId))) {
                                                                stepActivityService.log(
                                                                                step,
                                                                                actor,
                                                                                JobWorkflowStepActivityType.WORKER_REMOVED,
                                                                                "Removed worker ID " + oldId);
                                                        }
                                                }
                                        }

                                        incomingIds.add(step.getId());
                                }

                                /*
                                 * ============================
                                 * CREATE NEW STEP
                                 * ============================
                                 */
                                else {

                                        JobWorkflowStep newStep = JobWorkflowStep.builder()
                                                        .jobWorkflow(jw)
                                                        .name(sr.getName())
                                                        .description(sr.getDescription())
                                                        .orderIndex(sr.getOrderIndex())
                                                        .status(status)
                                                        .startedAt(startedAt)
                                                        .completedAt(completedAt)
                                                        .build();

                                        if (sr.getAssignedWorkerIds() != null) {
                                                List<Worker> workerList = workerRepository
                                                                .findAllById(sr.getAssignedWorkerIds());
                                                if (workerList.size() != sr.getAssignedWorkerIds().size()) {
                                                        throw new WorkerNotFoundException(
                                                                        "One or more workers not found");
                                                }
                                                for (Worker w : workerList) {
                                                        if (!w.getCompany().getId().equals(companyId)) {
                                                                throw new UnauthorizedWorkflowAccessException(
                                                                                "Worker does not belong to company");
                                                        }
                                                }
                                                newStep.getAssignedWorkers().addAll(workerList);
                                        }

                                        jobWorkflowStepRepository.save(newStep);

                                        stepActivityService.log(
                                                        newStep,
                                                        actor,
                                                        JobWorkflowStepActivityType.STEP_CREATED,
                                                        "Created workflow step");
                                }
                        }
                }

                /*
                 * ============================
                 * DELETE REMOVED STEPS
                 * ============================
                 */
                for (

                JobWorkflowStep step : existingSteps) {
                        if (!incomingIds.contains(step.getId())) {

                                /*
                                 * classic Hibernate flush-order /
                                 * transient reference problem
                                 * stepActivityService.log(
                                 * step,
                                 * actor,
                                 * JobWorkflowStepActivityType.STEP_UPDATED,
                                 * "Deleted workflow step");
                                 */

                                jobWorkflowStepRepository.delete(step);
                        }
                }

                /*
                 * ============================
                 * NORMALIZE ORDER (1-BASED)
                 * ============================
                 */
                normalizeOrderIndexes(jw.getId());

                /*
                 * ============================
                 * UPDATE WORKFLOW STATUS
                 * ============================
                 */
                updateJobWorkflowStatus(jw);

                return buildResponse(jw);
        }
        /*
         * =======================
         * ASSIGN WORKER TO ALL STEPS
         * =======================
         */

        @Override
        @Transactional
        public JobWorkflowResponse assignAWorkerToAllSteps(
                        Long jobWorkflowId, Long workerId, Long companyId) {

                JobWorkflow jobWorkflow = jobWorkflowRepository.findById(jobWorkflowId)
                                .orElseThrow(() -> new JobWorkflowNotFoundException("Job workflow not found"));
                if (!jobWorkflow.getJob().getCompany().getId().equals(companyId)) {
                        throw new UnauthorizedWorkflowAccessException(
                                        "Unauthorized access");
                }
                Worker worker = workerRepository.findById(workerId)
                                .orElseThrow(() -> new WorkerNotFoundException("Worker not found"));
                if (!worker.getCompany().getId().equals(companyId)) {
                        throw new UnauthorizedWorkflowAccessException(
                                        "Worker does not belong to company");
                }
                List<JobWorkflowStep> steps = jobWorkflowStepRepository.findByJobWorkflowIdOrderByOrderIndexAsc(
                                jobWorkflow.getId());

                for (JobWorkflowStep step : steps) {
                        step.getAssignedWorkers().add(worker);
                }

                // Batch-save all log entries in one saveAll call
                stepActivityService.logAll(
                                steps,
                                jobWorkflow.getJob().getCompany().getUser(),
                                JobWorkflowStepActivityType.WORKER_ASSIGNED,
                                "Assigned worker ID " + worker.getId());

                updateJobWorkflowStatus(jobWorkflow);
                return buildResponse(jobWorkflow);
        }

        /*
         * =======================
         * ASSIGN MULTIPLE WORKERS TO ALL STEPS
         * =======================
         */

        @Override
        @Transactional
        public JobWorkflowResponse assignWorkersToAllSteps(
                        Long jobWorkflowId, List<Long> workerIds, Long companyId) {

                JobWorkflow jobWorkflow = jobWorkflowRepository.findById(jobWorkflowId)
                                .orElseThrow(() -> new JobWorkflowNotFoundException("Job workflow not found"));

                if (!jobWorkflow.getJob().getCompany().getId().equals(companyId)) {
                        throw new UnauthorizedWorkflowAccessException("Unauthorized access");
                }

                // If the list is null or empty, we can just return early
                if (workerIds == null || workerIds.isEmpty()) {
                        return buildResponse(jobWorkflow);
                }

                // Fetch and validate all workers
                List<Worker> workers = workerRepository.findAllById(workerIds);
                if (workers.size() != workerIds.size()) {
                        throw new WorkerNotFoundException("One or more workers not found");
                }

                for (Worker worker : workers) {
                        if (!worker.getCompany().getId().equals(companyId)) {
                                throw new UnauthorizedWorkflowAccessException(
                                                "Worker ID " + worker.getId() + " does not belong to company");
                        }
                }

                List<JobWorkflowStep> steps = jobWorkflowStepRepository.findByJobWorkflowIdOrderByOrderIndexAsc(
                                jobWorkflow.getId());

                // Assign all workers to all steps
                for (JobWorkflowStep step : steps) {
                        step.getAssignedWorkers().addAll(workers);
                }

                User actor = jobWorkflow.getJob().getCompany().getUser();

                // Batch-save log entries for each worker
                for (Worker worker : workers) {
                        stepActivityService.logAll(
                                        steps,
                                        actor,
                                        JobWorkflowStepActivityType.WORKER_ASSIGNED,
                                        "Assigned worker ID " + worker.getId());
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
                                .orElseThrow(() -> new JobWorkflowNotFoundException("Job workflow not found"));
                if (!jobWorkflow.getJob().getCompany().getId().equals(companyId)) {
                        throw new UnauthorizedWorkflowAccessException("Job does not belong to company");
                }
                jobWorkflowStepRepository.deleteByJobWorkflowId(jobWorkflow.getId());
                jobWorkflowRepository.delete(jobWorkflow);
        }

        @Override
        @Transactional
        public JobWorkflowStepResponse addStep(
                        Long jobWorkflowId,
                        JobWorkflowStepCreateRequest request,
                        Long companyId) {

                JobWorkflow jw = jobWorkflowRepository.findById(jobWorkflowId)
                                .orElseThrow(() -> new JobWorkflowNotFoundException("Job workflow not found"));

                if (!jw.getJob().getCompany().getId().equals(companyId)) {
                        throw new UnauthorizedWorkflowAccessException("Unauthorized access");
                }

                User actor = jw.getJob().getCompany().getUser();

                // Determine order index (1-based)
                int orderIndex;
                if (request.getOrderIndex() != null) {
                        orderIndex = request.getOrderIndex();
                } else {
                        orderIndex = jobWorkflowStepRepository
                                        .findMaxOrderIndexByJobWorkflowId(jw.getId()) + 1;
                }

                WorkflowStepStatus status = request.getStatus() != null
                                ? request.getStatus()
                                : WorkflowStepStatus.NOT_STARTED;

                LocalDateTime startedAt = status == WorkflowStepStatus.STARTED ? LocalDateTime.now() : null;
                LocalDateTime completedAt = status == WorkflowStepStatus.COMPLETED ? LocalDateTime.now() : null;

                JobWorkflowStep step = JobWorkflowStep.builder()
                                .jobWorkflow(jw)
                                .name(request.getName())
                                .description(request.getDescription())
                                .orderIndex(orderIndex)
                                .status(status)
                                .startedAt(startedAt)
                                .completedAt(completedAt)
                                .build();

                // Assign workers
                if (request.getAssignedWorkerIds() != null) {
                        List<Worker> workerList = workerRepository.findAllById(request.getAssignedWorkerIds());
                        if (workerList.size() != request.getAssignedWorkerIds().size()) {
                                throw new WorkerNotFoundException("One or more workers not found");
                        }
                        for (Worker w : workerList) {
                                if (!w.getCompany().getId().equals(companyId)) {
                                        throw new UnauthorizedWorkflowAccessException(
                                                        "Worker does not belong to company");
                                }
                        }
                        step.getAssignedWorkers().addAll(workerList);
                }

                jobWorkflowStepRepository.save(step);

                logStep(
                                step,
                                actor,
                                JobWorkflowStepActivityType.STEP_CREATED,
                                "Created workflow step");

                // Normalize order & update workflow status
                normalizeOrderIndexes(jw.getId());
                updateJobWorkflowStatus(jw);

                return mapStep(step);
        }

        /*
         * =======================
         * MAPPERS — delegated to shared JobWorkflowMapper
         * =======================
         */
        private JobWorkflowStepResponse mapStep(JobWorkflowStep step) {
                return jobWorkflowMapper.mapStep(step);
        }

        private JobWorkflowResponse buildResponse(JobWorkflow jw) {
                return jobWorkflowMapper.buildWorkflowResponse(jw);
        }

        private JobWorkflowResponse buildResponse(JobWorkflow jw, List<JobWorkflowStep> steps) {
                return jobWorkflowMapper.buildWorkflowResponse(jw, steps);
        }
}