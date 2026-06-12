package com.workflow.service.workflow;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.workflow.common.constant.workflow.JobWorkflowStepActivityType;
import com.workflow.common.constant.workflow.StepDiscussionType;
import com.workflow.common.constant.workflow.WorkflowStepStatus;
import com.workflow.common.exception.business.EmptyFileException;
import com.workflow.common.exception.business.FileSizeLimitExceededException;
import com.workflow.common.exception.business.ForbiddenActionException;
import com.workflow.common.exception.business.InvalidTimeLogException;
import com.workflow.common.exception.business.JobWorkflowNotFoundException;
import com.workflow.common.exception.business.WorkerNotFoundException;
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
import com.workflow.dto.workflow.StepVisitLogSummaryResponse;
import com.workflow.dto.workflow.WorkerAssignedStepResponse;
import com.workflow.entity.asset.AssetJobAssignment;
import com.workflow.entity.common.Address;
import com.workflow.entity.customer.Customer;
import com.workflow.entity.customer.CustomerAddress;
import com.workflow.entity.job.Job;
import com.workflow.entity.job.JobWorkflow;
import com.workflow.entity.job.JobWorkflowStep;
import com.workflow.entity.job.JobWorkflowStepAttachment;
import com.workflow.entity.job.JobWorkflowStepComment;
import com.workflow.entity.job.JobWorkflowStepVisitLog;
import com.workflow.entity.worker.Worker;
import com.workflow.repository.asset.AssetJobAssignmentRepository;
import com.workflow.repository.job.JobWorkflowRepository;
import com.workflow.repository.job.JobWorkflowStepAttachmentRepository;
import com.workflow.repository.job.JobWorkflowStepCommentRepository;
import com.workflow.repository.job.JobWorkflowStepRepository;
import com.workflow.repository.job.JobWorkflowStepVisitLogRepository;
import com.workflow.repository.worker.WorkerRepository;
import com.workflow.service.storage.IStorageService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkerJobWorkflowService implements IWorkerJobWorkflowService {

        private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

        private final WorkerRepository workerRepository;
        private final JobWorkflowStepRepository stepRepository;
        private final JobWorkflowRepository jobWorkflowRepository;
        private final JobWorkflowStepCommentRepository commentRepository;
        private final JobWorkflowStepAttachmentRepository attachmentRepository;
        private final JobWorkflowStepVisitLogRepository visitLogRepository;
        private final AssetJobAssignmentRepository assignmentRepository;
        private final Tika tika;

        private final IStepActivityService stepActivityService;
        private final IStorageService s3Service;
        private final JobWorkflowMapper jobWorkflowMapper;

        // Spring injects the list from application.yml here!
        @Value("${workflow.security.file.blocked-types}")
        private List<String> blockedTypes;

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
        public List<WorkerAssignedStepResponse> getMyAssignedSteps(Long workerUserId) {
                Worker worker = getWorker(workerUserId);

                // JOIN FETCH ensures jobWorkflow and job are loaded in one query (no lazy
                // chain)
                List<JobWorkflowStep> assignedSteps = stepRepository
                                .findByAssignedWorkers_IdAndJobNotArchived(worker.getId());

                // Batch-load all active asset assignments for the relevant jobs in one query
                List<Long> jobIds = assignedSteps.stream()
                                .map(s -> s.getJobWorkflow().getJob().getId())
                                .distinct()
                                .collect(Collectors.toList());

                Map<Long, List<AssetAssignmentResponse>> assetsByJobId = assignmentRepository
                                .findByJobIdInAndReturnedAtIsNull(jobIds)
                                .stream()
                                .collect(Collectors.groupingBy(
                                                a -> a.getJob().getId(),
                                                Collectors.mapping(this::mapAssetAssignment, Collectors.toList())));

                return assignedSteps.stream()
                                .<WorkerAssignedStepResponse>map(step -> {
                                        Job job = step.getJobWorkflow().getJob();
                                        return WorkerAssignedStepResponse.builder()
                                                        .step(mapStep(step))
                                                        .jobId(job.getId())
                                                        .jobRef(job.getJobRef())
                                                        .jobAddress(mapJobAddress(job.getAddress()))
                                                        .customer(mapCustomer(job.getCustomer()))
                                                        .assignedAssets(assetsByJobId.getOrDefault(job.getId(),
                                                                        new ArrayList<>()))
                                                        .build();
                                })
                                .collect(Collectors.toList());
        }

        @Override
        @Transactional(readOnly = true)
        public List<JobWorkflowResponse> getAssignedJobWorkflows(Long workerUserId) {
                Worker worker = getWorker(workerUserId);

                // JOIN FETCH loads jobWorkflow + job in one query
                List<JobWorkflowStep> assignedSteps = stepRepository.findByAssignedWorkers_Id(worker.getId());

                // Extract distinct JobWorkflows
                List<JobWorkflow> workflows = assignedSteps.stream()
                                .map(JobWorkflowStep::getJobWorkflow)
                                .distinct()
                                .toList();

                if (workflows.isEmpty())
                        return new ArrayList<>();

                // Batch-load all steps for all workflows in one query, then group by workflowId
                List<Long> workflowIds = workflows.stream().map(JobWorkflow::getId).collect(Collectors.toList());
                Map<Long, List<JobWorkflowStep>> stepsByWorkflowId = stepRepository
                                .findByJobWorkflowIdInOrderByOrderIndexAsc(workflowIds)
                                .stream()
                                .collect(Collectors.groupingBy(s -> s.getJobWorkflow().getId()));

                return workflows.stream()
                                .map(jw -> buildWorkflowResponse(jw,
                                                stepsByWorkflowId.getOrDefault(jw.getId(), new ArrayList<>())))
                                .toList();
        }

        @Override
        @Transactional(readOnly = true)
        public JobWorkflowResponse getJobWorkflowIfAssigned(Long jobWorkflowId, Long workerUserId) {
                Worker worker = getWorker(workerUserId);

                // Single existence check query — avoids loading all assigned steps
                boolean isAssignedToWorkflow = stepRepository.existsByJobWorkflowIdAndWorkerId(
                                jobWorkflowId, worker.getId());

                if (!isAssignedToWorkflow) {
                        throw new ForbiddenActionException("You do not have access to this Job Workflow.");
                }

                // Load the full workflow to build the response
                JobWorkflow jw = jobWorkflowRepository.findById(jobWorkflowId)
                                .orElseThrow(() -> new JobWorkflowNotFoundException("Job Workflow not found"));

                return buildWorkflowResponse(jw);
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
                                                .authorUsername(c.getAuthor().getUsername())
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
                                                .fileUrl(s3Service.resolveFileUrl(a.getFileUrl()))
                                                .description(a.getDescription())
                                                .type(a.getType())
                                                .uploadedBy(a.getUploadedBy().getId())
                                                .uploadedByUsername(a.getUploadedBy().getUsername())
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

                return StepTimelineBuilder.build(
                                commentRepository.findByStepIdOrderByCreatedAtAsc(stepId),
                                attachmentRepository.findByStepIdOrderByCreatedAtAsc(stepId),
                                s3Service::resolveFileUrl);
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

                // Start clock only if null, clear completedAt just in case of a reset
                if (step.getStartedAt() == null) {
                        step.setStartedAt(LocalDateTime.now(ZoneOffset.UTC));
                }
                step.setCompletedAt(null);

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

                checkStepStatusTransition(step, WorkflowStepStatus.INITIATED);

                step.setStatus(WorkflowStepStatus.ONGOING);

                // SLA FIX: Start clock only if null, clear completedAt
                if (step.getStartedAt() == null) {
                        step.setStartedAt(LocalDateTime.now(ZoneOffset.UTC));
                }
                step.setCompletedAt(null);

                if (step.getJobWorkflow().getStatus() == WorkflowStepStatus.INITIATED) {
                        step.getJobWorkflow().setStatus(WorkflowStepStatus.ONGOING);
                }

                stepRepository.save(step);

                stepActivityService.log(step, worker.getUser(), JobWorkflowStepActivityType.STATUS_CHANGED,
                                "Worker " + worker.getName() + " marked the step as ONGOING.");

                return mapStep(step);
        }

        @Override
        public JobWorkflowStepResponse completeOngoingStep(Long stepId, Long workerUserId) {
                Worker worker = getWorker(workerUserId);
                JobWorkflowStep step = getAssignedStep(stepId, worker.getId());

                checkStepStatusTransition(step, WorkflowStepStatus.ONGOING);

                step.setStatus(WorkflowStepStatus.COMPLETED);
                step.setCompletedAt(LocalDateTime.now(ZoneOffset.UTC));

                // SLA FIX: Edge case catch
                if (step.getStartedAt() == null) {
                        step.setStartedAt(LocalDateTime.now(ZoneOffset.UTC));
                }

                stepRepository.save(step);

                stepActivityService.log(step, worker.getUser(), JobWorkflowStepActivityType.STATUS_CHANGED,
                                "Worker " + worker.getName() + " marked the ongoing step as COMPLETED.");

                checkAndUpdateParentWorkflowStatus(step.getJobWorkflow());

                return mapStep(step);
        }

        @Override
        public JobWorkflowStepResponse completeStep(Long stepId, Long workerUserId) {
                Worker worker = getWorker(workerUserId);
                JobWorkflowStep step = getAssignedStep(stepId, worker.getId());

                checkStepStatusTransition(step, WorkflowStepStatus.STARTED);

                step.setStatus(WorkflowStepStatus.COMPLETED);
                step.setCompletedAt(LocalDateTime.now(ZoneOffset.UTC));

                // SLA FIX: Edge case catch
                if (step.getStartedAt() == null) {
                        step.setStartedAt(LocalDateTime.now(ZoneOffset.UTC));
                }

                stepRepository.save(step);

                stepActivityService.log(step, worker.getUser(), JobWorkflowStepActivityType.STATUS_CHANGED,
                                "Worker " + worker.getName() + " completed the step.");

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

                // Check if every single step is safely resolved (COMPLETED or SKIPPED)
                boolean allCompleted = allSteps.stream()
                                .allMatch(s -> s.getStatus() == WorkflowStepStatus.COMPLETED ||
                                                s.getStatus() == WorkflowStepStatus.SKIPPED);

                if (allCompleted) {
                        jobWorkflow.setStatus(WorkflowStepStatus.COMPLETED);
                        jobWorkflow.setCompletedAt(LocalDateTime.now(ZoneOffset.UTC));
                        // Explicit save makes the intent clear and guards against future
                        // transaction boundary changes that might move this logic outside a
                        // managed persistence context.
                        jobWorkflowRepository.save(jobWorkflow);
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
                                .authorUsername(comment.getAuthor().getUsername())
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

                // 1. Detect true file type securely
                String detectedType = tika.detect(file.getInputStream());

                if (blockedTypes.contains(detectedType)) {
                        throw new ForbiddenActionException(
                                        "Upload is blocked for security reasons.");
                }

                Worker worker = getWorker(workerUserId);
                JobWorkflowStep step = getAssignedStep(stepId, worker.getId());

                // 2. Safely extract extension and generate UUID for S3 Key
                String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
                String extension = "";
                if (originalFilename.contains(".")) {
                        extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                }
                String safeUniqueFilename = UUID.randomUUID().toString() + extension;

                // 3. Build S3 key using the UUID and worker-specific path
                String key = String.format(
                                "companies/%d/jobs/%d/steps/%d/workers/%d/%s",
                                step.getJobWorkflow().getJob().getCompany().getId(),
                                step.getJobWorkflow().getJob().getId(),
                                step.getId(),
                                worker.getId(),
                                safeUniqueFilename); // <-- UUID used here

                // 4. Upload using the secure detectedType
                s3Service.upload(
                                key,
                                file.getInputStream(),
                                file.getSize(),
                                detectedType); // <-- Secure type used here

                // 5. Save to database
                JobWorkflowStepAttachment attachment = attachmentRepository.save(
                                JobWorkflowStepAttachment.builder()
                                                .step(step)
                                                .uploadedBy(worker.getUser())
                                                .fileName(originalFilename) // <-- Safe original name for UI
                                                .fileType(detectedType) // <-- Secure type used here
                                                .fileUrl(key) // <-- UUID path used here
                                                .type(type)
                                                .description(description)
                                                .build());

                stepActivityService.log(step, worker.getUser(), JobWorkflowStepActivityType.ATTACHMENT_ADDED,
                                "Worker uploaded " + originalFilename);

                return StepAttachmentResponse.builder()
                                .id(attachment.getId())
                                .fileName(attachment.getFileName())
                                .fileType(attachment.getFileType())
                                .fileUrl(s3Service.resolveFileUrl(attachment.getFileUrl()))
                                .description(attachment.getDescription())
                                .type(attachment.getType())
                                .uploadedBy(attachment.getUploadedBy().getId())
                                .uploadedByUsername(attachment.getUploadedBy().getUsername())
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
        // MAPPERS — delegated to shared JobWorkflowMapper
        // ==========================================

        private JobWorkflowResponse buildWorkflowResponse(JobWorkflow jw) {
                return jobWorkflowMapper.buildWorkflowResponse(jw);
        }

        private JobWorkflowResponse buildWorkflowResponse(JobWorkflow jw, List<JobWorkflowStep> steps) {
                return jobWorkflowMapper.buildWorkflowResponse(jw, steps);
        }

        private JobWorkflowStepResponse mapStep(JobWorkflowStep step) {
                return jobWorkflowMapper.mapStep(step);
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
                long durationDays = a.isActive()
                                ? nullSafeDaysBetween(a.getAssignedAt(), LocalDateTime.now(ZoneOffset.UTC))
                                : nullSafeDaysBetween(a.getAssignedAt(), a.getReturnedAt());

                String status = a.isActive() ? "ACTIVE" : "COMPLETED";

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