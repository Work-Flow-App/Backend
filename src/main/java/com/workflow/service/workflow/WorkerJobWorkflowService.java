package com.workflow.service.workflow;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.workflow.common.constant.workflow.JobWorkflowStepActivityType;
import com.workflow.common.constant.workflow.WorkflowStepStatus;
import com.workflow.common.exception.business.ForbiddenActionException;
import com.workflow.common.exception.business.JobWorkflowNotFoundException;
import com.workflow.common.exception.business.WorkerNotFoundException;
import com.workflow.dto.workflow.JobWorkflowResponse;
import com.workflow.dto.workflow.JobWorkflowStepResponse;
import com.workflow.dto.workflow.StepAttachmentResponse;
import com.workflow.dto.workflow.StepCommentCreateRequest;
import com.workflow.dto.workflow.StepCommentResponse;
import com.workflow.dto.workflow.StepTimelineItemResponse;
import com.workflow.entity.JobWorkflow;
import com.workflow.entity.JobWorkflowStep;
import com.workflow.entity.JobWorkflowStepAttachment;
import com.workflow.entity.JobWorkflowStepComment;
import com.workflow.entity.Worker;
import com.workflow.repository.JobWorkflowStepAttachmentRepository;
import com.workflow.repository.JobWorkflowStepCommentRepository;
import com.workflow.repository.JobWorkflowStepRepository;
import com.workflow.repository.WorkerRepository;
import com.workflow.service.storage.S3StorageService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkerJobWorkflowService implements IWorkerJobWorkflowService {

        private final WorkerRepository workerRepository;
        private final JobWorkflowStepRepository stepRepository;
        private final JobWorkflowStepCommentRepository commentRepository;
        private final JobWorkflowStepAttachmentRepository attachmentRepository;
        private final IStepActivityService stepActivityService;
        private final S3StorageService s3Service;

        // Reuse the mapping logic from JobWorkflowService to maintain consistency
        // Ideally, this should be in a shared Mapper component, but implemented here
        // for self-containment.

        // ==========================================
        // INTERNAL HELPERS
        // ==========================================

        private Worker getWorker(Long userId) {
                return workerRepository.findByUserId(userId)
                                .orElseThrow(() -> new WorkerNotFoundException(
                                                "Current user is not a registered worker"));
        }

        private JobWorkflowStep getAssignedStep(Long stepId, Long workerId) {
                return stepRepository.findByIdAndWorkerId(stepId, workerId)
                                .orElseThrow(
                                                () -> new ForbiddenActionException(
                                                                "You are not assigned to this step or it does not exist."));
        }

        private void checkStepStatusTransition(JobWorkflowStep step, WorkflowStepStatus requiredCurrentStatus) {
                if (step.getStatus() != requiredCurrentStatus) {
                        throw new ForbiddenActionException(
                                        String.format("Cannot change status. Current status is %s, but required status is %s",
                                                        step.getStatus(), requiredCurrentStatus));
                }
        }

        // ==========================================
        // READ OPERATIONS
        // ==========================================

        @Override
        @Transactional(readOnly = true)
        public List<JobWorkflowResponse> getAssignedJobWorkflows(Long workerUserId) {
                Worker worker = getWorker(workerUserId);

                // Find all steps assigned to worker
                List<JobWorkflowStep> assignedSteps = stepRepository.findByAssignedWorkers_Id(worker.getId());

                // Extract distinct JobWorkflows
                List<JobWorkflow> workflows = assignedSteps.stream()
                                .map(JobWorkflowStep::getJobWorkflow)
                                .distinct()
                                .toList();

                return workflows.stream()
                                .map(this::buildWorkflowResponse)
                                .toList();
        }

        @Override
        @Transactional(readOnly = true)
        public JobWorkflowResponse getJobWorkflowIfAssigned(Long jobWorkflowId, Long workerUserId) {
                Worker worker = getWorker(workerUserId);

                // We fetch all steps for this worker to see if they are involved in this
                // workflow at all
                List<JobWorkflowStep> assignedSteps = stepRepository.findByAssignedWorkers_Id(worker.getId());

                boolean isAssignedToWorkflow = assignedSteps.stream()
                                .anyMatch(s -> s.getJobWorkflow().getId().equals(jobWorkflowId));

                if (!isAssignedToWorkflow) {
                        throw new ForbiddenActionException("You do not have access to this Job Workflow.");
                }

                // Return the full jobworkflow details (Worker can see the whole flow context,
                // even
                // steps they aren't assigned to)
                // If you want to restrict them to ONLY see their steps, filter the steps list
                // in buildWorkflowResponse.
                JobWorkflowStep step = assignedSteps.stream()
                                .filter(s -> s.getJobWorkflow().getId().equals(jobWorkflowId))
                                .findFirst()
                                .orElseThrow(() -> new JobWorkflowNotFoundException("Job Workflow not found"));

                return buildWorkflowResponse(step.getJobWorkflow());
        }

        @Override
        @Transactional(readOnly = true)
        public JobWorkflowStepResponse getStepIfAssigned(Long stepId, Long workerUserId) {
                Worker worker = getWorker(workerUserId);
                JobWorkflowStep step = getAssignedStep(stepId, worker.getId());
                return mapStep(step);
        }

        @Override
        @Transactional(readOnly = true)
        public List<StepTimelineItemResponse> getStepTimeline(Long stepId, Long workerUserId) {
                Worker worker = getWorker(workerUserId);
                // Verify assignment
                getAssignedStep(stepId, worker.getId());

                // Reusing logic similar to JobWorkflowStepActivityService but secured for
                // worker
                List<StepTimelineItemResponse> comments = commentRepository.findByStepIdOrderByCreatedAtAsc(stepId)
                                .stream()
                                .map(c -> StepTimelineItemResponse.builder()
                                                .id(c.getId())
                                                .itemType("COMMENT")
                                                .content(c.getContent())
                                                .actorId(c.getAuthor().getId())
                                                .createdAt(c.getCreatedAt())
                                                .build())
                                .toList();

                List<StepTimelineItemResponse> attachments = attachmentRepository
                                .findByStepIdOrderByCreatedAtAsc(stepId)
                                .stream()
                                .map(a -> StepTimelineItemResponse.builder()
                                                .id(a.getId())
                                                .itemType("ATTACHMENT")
                                                .content(a.getFileName())
                                                .fileUrl(a.getFileUrl())
                                                .actorId(a.getUploadedBy().getId())
                                                .createdAt(a.getCreatedAt())
                                                .build())
                                .toList();

                return java.util.stream.Stream.concat(comments.stream(), attachments.stream())
                                .sorted(java.util.Comparator.comparing(StepTimelineItemResponse::getCreatedAt))
                                .toList();
        }

        // ==========================================
        // STATUS ACTIONS
        // ==========================================

        @Override
        public JobWorkflowStepResponse startStep(Long stepId, Long workerUserId) {
                Worker worker = getWorker(workerUserId);
                JobWorkflowStep step = getAssignedStep(stepId, worker.getId());

                checkStepStatusTransition(step, WorkflowStepStatus.NOT_STARTED);

                step.setStatus(WorkflowStepStatus.STARTED);
                step.setStartedAt(LocalDateTime.now());

                // If the parent workflow was NOT_STARTED, it should technically become STARTED
                // now.
                // We can update parent status:
                if (step.getJobWorkflow().getStatus() == WorkflowStepStatus.NOT_STARTED) {
                        step.getJobWorkflow().setStatus(WorkflowStepStatus.STARTED);
                }

                stepRepository.save(step);

                stepActivityService.log(step, worker.getUser(), JobWorkflowStepActivityType.STATUS_CHANGED,
                                "Worker " + worker.getName() + " started the step.");

                return mapStep(step);
        }

        // ==========================================
        // STATUS ACTIONS
        // ==========================================

        @Override
        public JobWorkflowStepResponse completeStep(Long stepId, Long workerUserId) {
                Worker worker = getWorker(workerUserId);
                JobWorkflowStep step = getAssignedStep(stepId, worker.getId());

                // 1. Validate transition
                checkStepStatusTransition(step, WorkflowStepStatus.STARTED);

                // 2. Update Step Status
                step.setStatus(WorkflowStepStatus.COMPLETED);
                step.setCompletedAt(LocalDateTime.now());
                stepRepository.save(step);

                // 3. Log Activity
                stepActivityService.log(step, worker.getUser(), JobWorkflowStepActivityType.STATUS_CHANGED,
                                "Worker " + worker.getName() + " completed the step.");

                // 4. Auto-complete Parent Workflow Check
                // We pass the parent workflow entity associated with this step
                checkAndUpdateParentWorkflowStatus(step.getJobWorkflow());

                return mapStep(step);
        }

        /**
         * Internal helper to check if the parent workflow should be marked COMPLETED.
         * This avoids circular dependency on JobWorkflowService.
         */
        private void checkAndUpdateParentWorkflowStatus(JobWorkflow jobWorkflow) {
                // We must fetch ALL steps for this workflow, not just the worker's assigned
                // steps.
                // JPA/Hibernate will use the repository we already have injected.
                List<JobWorkflowStep> allSteps = stepRepository
                                .findByJobWorkflowIdOrderByOrderIndexAsc(jobWorkflow.getId());

                // Check if every single step is COMPLETED
                boolean allCompleted = allSteps.stream()
                                .allMatch(s -> s.getStatus() == WorkflowStepStatus.COMPLETED);

                if (allCompleted) {
                        jobWorkflow.setStatus(WorkflowStepStatus.COMPLETED);
                        jobWorkflow.setCompletedAt(LocalDateTime.now());

                        // Since we are in a @Transactional method, modifying the attached
                        // 'jobWorkflow' entity will automatically sync to the DB on commit.
                        // We don't strictly need to call repository.save(jobWorkflow) here,
                        // but the state change happens immediately in the persistence context.
                }
        }

        // ==========================================
        // ACTIVITIES
        // ==========================================

        @Override
        public StepCommentResponse addComment(Long stepId, StepCommentCreateRequest request, Long workerUserId) {
                Worker worker = getWorker(workerUserId);
                JobWorkflowStep step = getAssignedStep(stepId, worker.getId());

                JobWorkflowStepComment comment = commentRepository.save(
                                JobWorkflowStepComment.builder()
                                                .step(step)
                                                .author(worker.getUser())
                                                .content(request.getContent())
                                                .build());

                stepActivityService.log(step, worker.getUser(), JobWorkflowStepActivityType.COMMENT,
                                request.getContent());

                return StepCommentResponse.builder()
                                .id(comment.getId())
                                .content(comment.getContent())
                                .authorId(comment.getAuthor().getId())
                                .createdAt(comment.getCreatedAt())
                                .updatedAt(comment.getUpdatedAt())
                                .build();
        }

        @Override
        public StepAttachmentResponse uploadAttachment(Long stepId, MultipartFile file, Long workerUserId)
                        throws IOException {
                Worker worker = getWorker(workerUserId);
                JobWorkflowStep step = getAssignedStep(stepId, worker.getId());

                String key = String.format("companies/%d/jobs/%d/steps/%d/workers/%d/%s",
                                step.getJobWorkflow().getJob().getCompany().getId(),
                                step.getJobWorkflow().getJob().getId(),
                                step.getId(),
                                worker.getId(),
                                file.getOriginalFilename());

                String url = s3Service.upload(key, file.getInputStream(), file.getSize(), file.getContentType());

                JobWorkflowStepAttachment attachment = attachmentRepository.save(
                                JobWorkflowStepAttachment.builder()
                                                .step(step)
                                                .uploadedBy(worker.getUser())
                                                .fileName(file.getOriginalFilename())
                                                .fileType(file.getContentType())
                                                .fileUrl(url)
                                                .build());

                stepActivityService.log(step, worker.getUser(), JobWorkflowStepActivityType.ATTACHMENT_ADDED,
                                "Worker uploaded " + file.getOriginalFilename());

                return StepAttachmentResponse.builder()
                                .id(attachment.getId())
                                .fileName(attachment.getFileName())
                                .fileType(attachment.getFileType())
                                .fileUrl(attachment.getFileUrl())
                                .uploadedBy(attachment.getUploadedBy().getId())
                                .createdAt(attachment.getCreatedAt())
                                .build();
        }

        // ==========================================
        // MAPPERS (Copied/Adapted for independence)
        // ==========================================

        private JobWorkflowResponse buildWorkflowResponse(JobWorkflow jw) {
                // Fetch fresh steps to ensure we have the list
                List<JobWorkflowStep> steps = stepRepository.findByJobWorkflowIdOrderByOrderIndexAsc(jw.getId());

                List<JobWorkflowStepResponse> stepResponses = steps.stream()
                                .map(this::mapStep)
                                .toList();

                return JobWorkflowResponse.builder()
                                .id(jw.getId())
                                .jobId(jw.getJob().getId())
                                .steps(stepResponses)
                                .status(jw.getStatus())
                                .build();
        }

        private JobWorkflowStepResponse mapStep(JobWorkflowStep step) {
                return JobWorkflowStepResponse.builder()
                                .id(step.getId())
                                .name(step.getName())
                                .description(step.getDescription())
                                .orderIndex(step.getOrderIndex())
                                .status(step.getStatus())
                                .startedAt(step.getStartedAt())
                                .completedAt(step.getCompletedAt())
                                .assignedWorkerIds(step.getAssignedWorkers().stream().map(Worker::getId)
                                                .collect(Collectors.toSet()))
                                .build();
        }
}