package com.workflow.service.workflow;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.workflow.common.constant.workflow.JobWorkflowStepActivityType;
import com.workflow.common.constant.workflow.StepDiscussionType;
import com.workflow.common.constant.workflow.WorkflowStepStatus;
import com.workflow.common.exception.business.EmptyFileException;
import com.workflow.common.exception.business.FileSizeLimitExceededException;
import com.workflow.common.exception.business.ForbiddenActionException;
import com.workflow.common.exception.business.JobWorkflowNotFoundException;
import com.workflow.common.exception.business.WorkerNotFoundException;
import com.workflow.common.exception.business.InvalidTimeLogException;
import com.workflow.dto.workflow.StepVisitLogSummaryResponse;
import com.workflow.dto.workflow.WorkerAssignedStepResponse;
import com.workflow.dto.asset.AssetAssignmentResponse;
import com.workflow.dto.customer.CustomerAddressDto;
import com.workflow.dto.customer.CustomerResponse;
import com.workflow.dto.job.AddressResponse;
import com.workflow.dto.workflow.JobWorkflowResponse;
import com.workflow.dto.workflow.JobWorkflowStepResponse;
import com.workflow.dto.workflow.StepAttachmentResponse;
import com.workflow.dto.workflow.StepCommentCreateRequest;
import com.workflow.dto.workflow.StepCommentResponse;
import com.workflow.dto.workflow.StepTimelineItemResponse;
import com.workflow.dto.workflow.StepVisitLogCreateRequest;
import com.workflow.dto.workflow.StepVisitLogResponse;
import com.workflow.entity.Address;
import com.workflow.entity.AssetJobAssignment;
import com.workflow.entity.Customer;
import com.workflow.entity.CustomerAddress;
import com.workflow.entity.Job;
import com.workflow.entity.JobWorkflow;
import com.workflow.entity.JobWorkflowStep;
import com.workflow.entity.JobWorkflowStepAttachment;
import com.workflow.entity.JobWorkflowStepComment;
import com.workflow.entity.JobWorkflowStepVisitLog;
import com.workflow.entity.Worker;
import com.workflow.repository.AssetJobAssignmentRepository;
import com.workflow.repository.JobWorkflowStepAttachmentRepository;
import com.workflow.repository.JobWorkflowStepCommentRepository;
import com.workflow.repository.JobWorkflowStepRepository;
import com.workflow.repository.JobWorkflowStepVisitLogRepository;
import com.workflow.repository.WorkerRepository;
import com.workflow.service.storage.S3StorageService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkerJobWorkflowService implements IWorkerJobWorkflowService {

        private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

        private final WorkerRepository workerRepository;
        private final JobWorkflowStepRepository stepRepository;
        private final JobWorkflowStepCommentRepository commentRepository;
        private final JobWorkflowStepAttachmentRepository attachmentRepository;
        private final JobWorkflowStepVisitLogRepository visitLogRepository;
        private final AssetJobAssignmentRepository assignmentRepository;

        private final IStepActivityService stepActivityService;
        private final S3StorageService s3Service;

        // Reuse the mapping logic from JobWorkflowService to maintain consistency
        // Ideally, this should be in a shared Mapper component, but implemented here
        // for self-containment.

        // ==========================================
        // INTERNAL HELPERS
        // ==========================================

        private String resolveFileUrl(String key) {
                return key == null ? null : s3Service.generatePresignedUrl(key);
        }

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
        public List<WorkerAssignedStepResponse> getMyAssignedSteps(Long workerUserId) {
                Worker worker = getWorker(workerUserId);

                List<JobWorkflowStep> assignedSteps = stepRepository.findByAssignedWorkers_Id(worker.getId());

                return assignedSteps.stream()
                                // 1. Explicitly tell the compiler what type we are mapping to
                                .<WorkerAssignedStepResponse>map(step -> {
                                        Job job = step.getJobWorkflow().getJob();
                                        Address jobAddress = job.getAddress();
                                        Customer customer = job.getCustomer();

                                        // 2. Break out the inner stream to help the compiler evaluate types
                                        // independently
                                        List<AssetJobAssignment> activeJobAssets = assignmentRepository
                                                        .findByJobIdAndReturnedAtIsNull(job.getId());

                                        // Map them directly without filtering by the specific worker
                                        List<AssetAssignmentResponse> jobAssets = activeJobAssets.stream()
                                                        .map(a -> mapAssetAssignment(a))
                                                        .collect(Collectors.toList());

                                        return WorkerAssignedStepResponse.builder()
                                                        .step(mapStep(step))
                                                        .jobId(job.getId())
                                                        .jobAddress(mapJobAddress(jobAddress))
                                                        .customer(mapCustomer(customer))
                                                        .assignedAssets(jobAssets)
                                                        .build();
                                })
                                .collect(Collectors.toList());
        }

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
        public List<StepCommentResponse> getStepComments(Long stepId, Long workerUserId) {
                Worker worker = getWorker(workerUserId);

                // Ensure the worker is assigned
                getAssignedStep(stepId, worker.getId());

                return commentRepository.findByStepIdOrderByCreatedAtAsc(stepId)
                                .stream()
                                .map(c -> StepCommentResponse.builder()
                                                .id(c.getId())
                                                .content(c.getContent())
                                                .type(c.getType())
                                                .authorId(c.getAuthor().getId())
                                                .createdAt(c.getCreatedAt())
                                                .updatedAt(c.getUpdatedAt())
                                                .build())
                                .toList();
        }

        @Override
        @Transactional(readOnly = true)
        public List<StepAttachmentResponse> getStepAttachments(Long stepId, Long workerUserId) {
                Worker worker = getWorker(workerUserId);

                // Ensure the worker is assigned
                getAssignedStep(stepId, worker.getId());

                return attachmentRepository.findByStepIdOrderByCreatedAtAsc(stepId)
                                .stream()
                                .map(a -> StepAttachmentResponse.builder()
                                                .id(a.getId())
                                                .fileName(a.getFileName())
                                                .fileType(a.getFileType())
                                                .fileUrl(resolveFileUrl(a.getFileUrl()))
                                                .description(a.getDescription())
                                                .type(a.getType())
                                                .uploadedBy(a.getUploadedBy().getId())
                                                .createdAt(a.getCreatedAt())
                                                .build())
                                .toList();
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
                                                .discussionType(c.getType())
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
                                                .fileUrl(resolveFileUrl(a.getFileUrl()))
                                                .discussionType(a.getType())
                                                .description(a.getDescription())
                                                .actorId(a.getUploadedBy().getId())
                                                .createdAt(a.getCreatedAt())
                                                .build())
                                .toList();

                return java.util.stream.Stream.concat(comments.stream(), attachments.stream())
                                .sorted(java.util.Comparator.comparing(StepTimelineItemResponse::getCreatedAt))
                                .toList();
        }

        @Override
        @Transactional(readOnly = true)
        public StepVisitLogSummaryResponse getVisitLogs(Long stepId, Long workerUserId) {
                Worker worker = getWorker(workerUserId);
                getAssignedStep(stepId, worker.getId());

                List<StepVisitLogResponse> logs = visitLogRepository.findByStepIdOrderByVisitDateDescTimeInDesc(stepId)
                                .stream()
                                .map(this::mapVisitLog)
                                .toList();

                // Calculate total minutes worked across all logs for this step
                Long totalMinutes = logs.stream()
                                .mapToLong(StepVisitLogResponse::getWorkedMinutes)
                                .sum();

                return StepVisitLogSummaryResponse.builder()
                                .visitLogs(logs)
                                .totalWorkedMinutes(totalMinutes)
                                .build();
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

        @Override
        public JobWorkflowStepResponse markStepOngoing(Long stepId, Long workerUserId) {
                Worker worker = getWorker(workerUserId);
                JobWorkflowStep step = getAssignedStep(stepId, worker.getId());

                // 1. Validate transition: Must be INITIATED to become ONGOING
                checkStepStatusTransition(step, WorkflowStepStatus.INITIATED);

                // 2. Update Step Status
                step.setStatus(WorkflowStepStatus.ONGOING);

                // Assuming you want to track when it actually became active
                if (step.getStartedAt() == null) {
                        step.setStartedAt(LocalDateTime.now());
                }

                // Optional: Update parent workflow status if it's also INITIATED
                if (step.getJobWorkflow().getStatus() == WorkflowStepStatus.INITIATED) {
                        step.getJobWorkflow().setStatus(WorkflowStepStatus.ONGOING);
                }

                stepRepository.save(step);

                // 3. Log Activity
                stepActivityService.log(step, worker.getUser(), JobWorkflowStepActivityType.STATUS_CHANGED,
                                "Worker " + worker.getName() + " marked the step as ONGOING.");

                return mapStep(step);
        }

        @Override
        public JobWorkflowStepResponse completeOngoingStep(Long stepId, Long workerUserId) {
                Worker worker = getWorker(workerUserId);
                JobWorkflowStep step = getAssignedStep(stepId, worker.getId());

                // 1. Validate transition: Must be ONGOING to become COMPLETED
                checkStepStatusTransition(step, WorkflowStepStatus.ONGOING);

                // 2. Update Step Status
                step.setStatus(WorkflowStepStatus.COMPLETED);
                step.setCompletedAt(LocalDateTime.now());
                stepRepository.save(step);

                // 3. Log Activity
                stepActivityService.log(step, worker.getUser(), JobWorkflowStepActivityType.STATUS_CHANGED,
                                "Worker " + worker.getName() + " marked the ongoing step as COMPLETED.");

                // 4. Auto-complete Parent Workflow Check
                checkAndUpdateParentWorkflowStatus(step.getJobWorkflow());

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
                                                .type(request.getType())
                                                .build());

                stepActivityService.log(step, worker.getUser(), JobWorkflowStepActivityType.COMMENT,
                                request.getContent());

                return StepCommentResponse.builder()
                                .id(comment.getId())
                                .content(comment.getContent())
                                .type(comment.getType())
                                .authorId(comment.getAuthor().getId())
                                .createdAt(comment.getCreatedAt())
                                .updatedAt(comment.getUpdatedAt())
                                .build();
        }

        @Override
        public StepAttachmentResponse uploadAttachment(
                        Long stepId,
                        MultipartFile file,
                        StepDiscussionType type,
                        String description,
                        Long workerUserId) throws IOException {

                if (file.isEmpty()) {
                        throw new EmptyFileException("Uploaded file cannot be empty");
                }

                if (file.getSize() > MAX_FILE_SIZE) {
                        throw new FileSizeLimitExceededException(
                                        "Attachment size must not exceed 10 MB");
                }

                Worker worker = getWorker(workerUserId);
                JobWorkflowStep step = getAssignedStep(stepId, worker.getId());

                String key = String.format(
                                "companies/%d/jobs/%d/steps/%d/workers/%d/%s",
                                step.getJobWorkflow().getJob().getCompany().getId(),
                                step.getJobWorkflow().getJob().getId(),
                                step.getId(),
                                worker.getId(),
                                file.getOriginalFilename());

                s3Service.upload(
                                key,
                                file.getInputStream(),
                                file.getSize(),
                                file.getContentType());

                JobWorkflowStepAttachment attachment = attachmentRepository.save(
                                JobWorkflowStepAttachment.builder()
                                                .step(step)
                                                .uploadedBy(worker.getUser())
                                                .fileName(file.getOriginalFilename())
                                                .fileType(file.getContentType())
                                                .fileUrl(key)
                                                .type(type)
                                                .description(description)
                                                .build());

                stepActivityService.log(step, worker.getUser(), JobWorkflowStepActivityType.ATTACHMENT_ADDED,
                                "Worker uploaded " + file.getOriginalFilename());

                return StepAttachmentResponse.builder()
                                .id(attachment.getId())
                                .fileName(attachment.getFileName())
                                .fileType(attachment.getFileType())
                                .fileUrl(resolveFileUrl(attachment.getFileUrl()))
                                .description(attachment.getDescription())
                                .type(attachment.getType())
                                .uploadedBy(attachment.getUploadedBy().getId())
                                .createdAt(attachment.getCreatedAt())
                                .build();
        }

        private void validateTimeLog(LocalTime timeIn, LocalTime timeOut) {
                if (timeIn != null && timeOut != null && timeOut.isBefore(timeIn)) {
                        throw new InvalidTimeLogException("End time cannot be before start time.");
                }
        }

        @Override
        public StepVisitLogResponse addVisitLog(Long stepId, StepVisitLogCreateRequest request, Long workerUserId) {
                // Validate time before processing
                validateTimeLog(request.getTimeIn(), request.getTimeOut());

                Worker worker = getWorker(workerUserId);
                JobWorkflowStep step = getAssignedStep(stepId, worker.getId());

                JobWorkflowStepVisitLog visitLog = visitLogRepository.save(
                                JobWorkflowStepVisitLog.builder()
                                                .step(step)
                                                .loggedBy(worker.getUser())
                                                .visitDate(request.getVisitDate())
                                                .timeIn(request.getTimeIn())
                                                .timeOut(request.getTimeOut())
                                                .description(request.getDescription())
                                                .build());

                stepActivityService.log(step, worker.getUser(), JobWorkflowStepActivityType.VISIT_LOGGED,
                                "Worker " + worker.getName() + " logged a visit for " + request.getVisitDate());

                return mapVisitLog(visitLog);
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

        private StepVisitLogResponse mapVisitLog(JobWorkflowStepVisitLog log) {
                // Calculate duration in minutes for the individual log
                Long duration = (log.getTimeIn() != null && log.getTimeOut() != null)
                                ? Duration.between(log.getTimeIn(), log.getTimeOut()).toMinutes()
                                : 0L;

                return StepVisitLogResponse.builder()
                                .id(log.getId())
                                .visitDate(log.getVisitDate())
                                .timeIn(log.getTimeIn())
                                .timeOut(log.getTimeOut())
                                .workedMinutes(duration) // Added mapped duration
                                .description(log.getDescription())
                                .loggedById(log.getLoggedBy().getId())
                                .createdAt(log.getCreatedAt())
                                .updatedAt(log.getUpdatedAt())
                                .build();
        }

        private CustomerResponse mapCustomer(Customer customer) {
                if (customer == null)
                        return null;
                return CustomerResponse.builder()
                                .id(customer.getId())
                                .name(customer.getName())
                                .email(customer.getEmail())
                                .telephone(customer.getTelephone())
                                .mobile(customer.getMobile())
                                .address(mapAddress(customer.getAddress()))
                                .archived(customer.isArchived())
                                .createdAt(customer.getCreatedAt())
                                .updatedAt(customer.getUpdatedAt())
                                .build();
        }

        private CustomerAddressDto mapAddress(CustomerAddress address) {
                if (address == null)
                        return null;
                return CustomerAddressDto.builder()
                                .houseNumber(address.getHouseNumber())
                                .street(address.getStreet())
                                .city(address.getCity())
                                .county(address.getCounty())
                                .postalCode(address.getPostalCode())
                                .country(address.getCountry())
                                .build();
        }

        private AssetAssignmentResponse mapAssetAssignment(AssetJobAssignment a) {
                long durationDays = a.getReturnedAt() == null
                                ? nullSafeDaysBetween(a.getAssignedAt(), LocalDateTime.now())
                                : nullSafeDaysBetween(a.getAssignedAt(), a.getReturnedAt());

                String status = a.getReturnedAt() == null ? "ACTIVE" : "COMPLETED";

                return AssetAssignmentResponse.builder()
                                .assignmentId(a.getId())
                                .assetId(a.getAsset().getId())
                                .jobId(a.getJob() != null ? a.getJob().getId() : null)
                                .assignedWorkerId(a.getAssignedWorker() != null ? a.getAssignedWorker().getId() : null)

                                // ✅ MAP ASSET DETAILS
                                .assetName(a.getAsset().getName())
                                .description(a.getAsset().getDescription())
                                .serialNumber(a.getAsset().getSerialNumber())
                                .assetTag(a.getAsset().getAssetTag())

                                .notes(a.getNotes())
                                .assignedAt(a.getAssignedAt())
                                .returnedAt(a.getReturnedAt())
                                .durationDays(durationDays)
                                .status(status)
                                .build();
        }

        private AddressResponse mapJobAddress(Address address) {
                if (address == null)
                        return null;

                return AddressResponse.builder()
                                .id(address.getId())
                                .street(address.getStreet())
                                .city(address.getCity())
                                .state(address.getState())
                                .postalCode(address.getPostalCode())
                                .country(address.getCountry())
                                .additionalInfo(address.getAdditionalInfo())
                                .latitude(address.getLatitude())
                                .longitude(address.getLongitude())
                                .build();
        }

        private long nullSafeDaysBetween(LocalDateTime from, LocalDateTime to) {
                if (from == null || to == null)
                        return 0L;
                return Duration.between(from, to).toDays();
        }
}