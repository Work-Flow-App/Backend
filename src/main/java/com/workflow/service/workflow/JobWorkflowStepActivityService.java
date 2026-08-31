package com.workflow.service.workflow;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.workflow.common.constant.notification.NotificationPriority;
import com.workflow.common.constant.notification.NotificationType;
import com.workflow.common.constant.workflow.JobWorkflowStepActivityType;
import com.workflow.common.constant.workflow.StepDiscussionType;
import com.workflow.common.exception.business.AttachmentNotFoundException;
import com.workflow.common.exception.business.CommentNotFoundException;
import com.workflow.common.exception.business.CompanyNotFoundException;
import com.workflow.common.exception.business.EmptyFileException;
import com.workflow.common.exception.business.FileSizeLimitExceededException;
import com.workflow.common.exception.business.ForbiddenActionException;
import com.workflow.common.exception.business.JobWorkflowStepNotFoundException;
import com.workflow.common.exception.business.UnauthorizedWorkflowAccessException;
import com.workflow.dto.workflow.StepActivityResponse;
import com.workflow.dto.workflow.StepAttachmentResponse;
import com.workflow.dto.workflow.StepAttachmentUpdateRequest;
import com.workflow.dto.workflow.StepCommentCreateRequest;
import com.workflow.dto.workflow.StepCommentResponse;
import com.workflow.dto.workflow.StepTimelineItemResponse;
import com.workflow.entity.company.Company;
import com.workflow.entity.job.JobWorkflowStep;
import com.workflow.entity.job.JobWorkflowStepActivity;
import com.workflow.entity.job.JobWorkflowStepAttachment;
import com.workflow.entity.job.JobWorkflowStepComment;
import com.workflow.entity.worker.Worker;
import com.workflow.repository.company.CompanyRepository;
import com.workflow.repository.job.JobWorkflowStepActivityRepository;
import com.workflow.repository.job.JobWorkflowStepAttachmentRepository;
import com.workflow.repository.job.JobWorkflowStepCommentRepository;
import com.workflow.repository.job.JobWorkflowStepRepository;
import com.workflow.service.notification.INotificationService;
import com.workflow.service.storage.IStorageService;
import com.workflow.service.subscription.IStorageQuotaService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class JobWorkflowStepActivityService
                implements IJobWorkflowStepActivityService {

        private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

        private final JobWorkflowStepRepository stepRepository;
        private final JobWorkflowStepCommentRepository commentRepository;
        private final JobWorkflowStepAttachmentRepository attachmentRepository;
        private final JobWorkflowStepActivityRepository activityRepository;
        private final CompanyRepository companyRepository;
        private final IStepActivityService stepActivityService;
        private final Tika tika;
        private final IStorageService s3Service;
        private final IStorageQuotaService storageQuotaService;
        private final INotificationService notificationService;

        // Spring injects the list from application.yml here!
        @Value("${workflow.security.file.blocked-types}")
        private List<String> blockedTypes;

        /*
         * ===========================
         * INTERNAL HELPERS
         * ===========================
         */

        private Company getCompany(Long companyId) {
                return companyRepository.findById(companyId)
                                .orElseThrow(() -> new CompanyNotFoundException("Company not found"));
        }

        private JobWorkflowStep getStep(Long stepId, Long companyId) {
                JobWorkflowStep step = stepRepository.findById(stepId)
                                .orElseThrow(() -> new JobWorkflowStepNotFoundException("Step not found"));

                if (!step.getJobWorkflow()
                                .getJob()
                                .getCompany()
                                .getId()
                                .equals(companyId)) {
                        throw new UnauthorizedWorkflowAccessException("Unauthorized access");
                }
                return step;
        }

        private void notifyAssignedWorkers(
                        JobWorkflowStep step,
                        NotificationType type,
                        String title,
                        String message,
                        String targetUrl,
                        String entityType,
                        Long entityId,
                        NotificationPriority priority,
                        Map<String, Object> extraMetadata) {

                // Send notification to all assigned workers
                for (Worker worker : step.getAssignedWorkers()) {
                        Map<String, Object> metadata = new HashMap<>(Map.of(
                                        "jobId", step.getJobWorkflow().getJob().getId(),
                                        "stepId", step.getId(),
                                        "workerId", worker.getId()));
                        if (extraMetadata != null) {
                                metadata.putAll(extraMetadata);
                        }

                        notificationService.createNotification(
                                        worker.getUser(),
                                        type,
                                        title,
                                        message,
                                        targetUrl,
                                        entityType,
                                        entityId,
                                        priority,
                                        metadata);
                }
        }

        /*
         * ===========================
         * COMMENTS
         * ===========================
         */

        @Override
        public StepCommentResponse addComment(
                        Long stepId,
                        StepCommentCreateRequest request,
                        Long companyId) {

                Company company = getCompany(companyId);
                JobWorkflowStep step = getStep(stepId, companyId);

                JobWorkflowStepComment comment = commentRepository.save(
                                JobWorkflowStepComment.builder()
                                                .step(step)
                                                .author(company.getUser())
                                                .content(request.getContent())
                                                .type(request.getType())
                                                .build());

                stepActivityService.log(step, company.getUser(), JobWorkflowStepActivityType.COMMENT,
                                request.getContent());

                notifyAssignedWorkers(step,
                                NotificationType.STEP_COMMENT_ADDED,
                                "New Company Comment",
                                String.format("A new comment was added to step '%s' (Job #%s).", step.getName(),
                                                step.getJobWorkflow().getJob().getJobRef()),
                                "/job-workflow-steps/" + step.getId() + "/discussion",
                                "JobWorkflowStepComment", comment.getId(),
                                NotificationPriority.LOW,
                                Map.of("commentId", comment.getId(), "discussionType", comment.getType().name(),
                                                "action", "OPEN_DISCUSSION"));
                return map(comment);
        }

        @Override
        public StepCommentResponse updateComment(
                        Long commentId,
                        StepCommentCreateRequest request,
                        Long companyId) {

                Company company = getCompany(companyId);

                JobWorkflowStepComment comment = commentRepository.findById(commentId)
                                .orElseThrow(() -> new CommentNotFoundException("Comment not found"));

                if (!comment.getAuthor().getId().equals(company.getUser().getId())) {
                        throw new ForbiddenActionException("Not allowed to edit this comment");
                }

                if (request.getContent() != null) {
                        comment.setContent(request.getContent());
                }

                if (request.getType() != null) {
                        comment.setType(request.getType());
                }

                stepActivityService.log(
                                comment.getStep(),
                                company.getUser(),
                                JobWorkflowStepActivityType.COMMENT,
                                "Edited a comment");

                notifyAssignedWorkers(comment.getStep(),
                                NotificationType.STEP_COMMENT_UPDATED,
                                "Company Comment Updated",
                                String.format("A comment on step '%s' (Job #%s) was updated.",
                                                comment.getStep().getName(),
                                                comment.getStep().getJobWorkflow().getJob().getJobRef()),
                                "/job-workflow-steps/" + comment.getStep().getId() + "/discussion",
                                "JobWorkflowStepComment", comment.getId(),
                                NotificationPriority.LOW,
                                Map.of("commentId", comment.getId(), "discussionType", comment.getType().name(),
                                                "action", "OPEN_DISCUSSION"));

                return map(comment);
        }

        @Override
        public void deleteComment(Long commentId, Long companyId) {

                Company company = getCompany(companyId);

                JobWorkflowStepComment comment = commentRepository.findById(commentId)
                                .orElseThrow(() -> new CommentNotFoundException("Comment not found"));

                if (!comment.getAuthor().getId().equals(company.getUser().getId())) {
                        throw new ForbiddenActionException("Not allowed to delete this comment");
                }

                stepActivityService.log(
                                comment.getStep(),
                                company.getUser(),
                                JobWorkflowStepActivityType.COMMENT,
                                "Deleted a comment");

                commentRepository.delete(comment);
        }

        @Override
        @Transactional(readOnly = true)
        public List<StepCommentResponse> getComments(Long stepId, Long companyId) {

                getStep(stepId, companyId);

                return commentRepository.findByStepIdOrderByCreatedAtAsc(stepId)
                                .stream()
                                .map(this::map)
                                .toList();
        }

        /*
         * ===========================
         * ATTACHMENTS
         * ===========================
         */
        @Override
        public StepAttachmentResponse uploadAttachment(
                        Long stepId,
                        MultipartFile file,
                        StepDiscussionType type,
                        String description,
                        Long companyId) throws IOException {

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

                Company company = getCompany(companyId);
                JobWorkflowStep step = getStep(stepId, companyId);

                // 2. Safely extract extension and generate UUID for S3 Key
                String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
                String extension = "";
                if (originalFilename.contains(".")) {
                        extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                }
                String safeUniqueFilename = UUID.randomUUID().toString() + extension;

                // 3. Build S3 key using the UUID
                String key = String.format(
                                "companies/%d/steps/%d/%s",
                                companyId,
                                stepId,
                                safeUniqueFilename); // <-- UUID used here

                // 4. Upload using the secure detectedType
                storageQuotaService.assertCapacity(companyId, file.getSize());
                s3Service.upload(
                                key,
                                file.getInputStream(),
                                file.getSize(),
                                detectedType); // <-- Secure type used here

                // 5. Save to database
                JobWorkflowStepAttachment attachment = attachmentRepository.save(
                                JobWorkflowStepAttachment.builder()
                                                .step(step)
                                                .uploadedBy(company.getUser())
                                                .fileName(originalFilename) // <-- Safe original name for UI
                                                .fileType(detectedType) // <-- Secure type used here
                                                .fileUrl(key) // <-- UUID path used here
                                                .fileSizeBytes(file.getSize())
                                                .type(type)
                                                .description(description)
                                                .build());
                storageQuotaService.recordUpload(companyId, file.getSize());

                stepActivityService.log(
                                step,
                                company.getUser(),
                                JobWorkflowStepActivityType.ATTACHMENT_ADDED,
                                "Uploaded " + originalFilename);

                notifyAssignedWorkers(step,
                                NotificationType.STEP_ATTACHMENT_ADDED,
                                "New Company Attachment",
                                String.format("A new attachment '%s' was uploaded to step '%s' (Job #%s).",
                                                originalFilename, step.getName(),
                                                step.getJobWorkflow().getJob().getJobRef()),
                                "/job-workflow-steps/" + step.getId() + "/discussion",
                                "JobWorkflowStepAttachment", attachment.getId(),
                                NotificationPriority.LOW,
                                Map.of("attachmentId", attachment.getId(), "discussionType",
                                                attachment.getType().name(), "action", "OPEN_DISCUSSION"));
                return map(attachment);
        }

        @Override
        public StepAttachmentResponse updateAttachment(
                        Long attachmentId,
                        StepAttachmentUpdateRequest request,
                        Long companyId) {

                Company company = getCompany(companyId);

                JobWorkflowStepAttachment attachment = attachmentRepository.findById(attachmentId)
                                .orElseThrow(() -> new AttachmentNotFoundException("Attachment not found"));

                if (!attachment.getUploadedBy().getId().equals(company.getUser().getId())) {
                        throw new ForbiddenActionException("Not allowed to edit this attachment");
                }

                if (request.getFileName() != null) {
                        attachment.setFileName(request.getFileName());
                }

                if (request.getDescription() != null) {
                        attachment.setDescription(request.getDescription());
                }

                if (request.getType() != null) {
                        attachment.setType(request.getType());
                }

                stepActivityService.log(
                                attachment.getStep(),
                                company.getUser(),
                                JobWorkflowStepActivityType.ATTACHMENT_UPDATED,
                                "Updated attachment: " + attachment.getFileName());

                notifyAssignedWorkers(attachment.getStep(),
                                NotificationType.STEP_ATTACHMENT_UPDATED,
                                "Company Attachment Updated",
                                String.format("An attachment ('%s') on step '%s' (Job #%s) was updated.",
                                                attachment.getFileName(), attachment.getStep().getName(),
                                                attachment.getStep().getJobWorkflow().getJob().getJobRef()),
                                "/job-workflow-steps/" + attachment.getStep().getId() + "/discussion",
                                "JobWorkflowStepAttachment", attachment.getId(),
                                NotificationPriority.LOW,
                                Map.of("attachmentId", attachment.getId(), "discussionType",
                                                attachment.getType().name(), "action", "OPEN_DISCUSSION"));

                return map(attachment);
        }

        @Override
        public void deleteAttachment(Long attachmentId, Long companyId) {

                Company company = getCompany(companyId);

                JobWorkflowStepAttachment attachment = attachmentRepository.findById(attachmentId)
                                .orElseThrow(() -> new AttachmentNotFoundException("Attachment not found"));

                if (!attachment.getUploadedBy().getId().equals(company.getUser().getId())) {
                        throw new ForbiddenActionException("Not allowed to delete this attachment");
                }

                s3Service.delete(attachment.getFileUrl());
                // Legacy rows uploaded before fileSizeBytes existed have nothing to decrement
                if (attachment.getFileSizeBytes() != null) {
                        storageQuotaService.recordDelete(companyId, attachment.getFileSizeBytes());
                }
                attachmentRepository.delete(attachment);

                stepActivityService.log(
                                attachment.getStep(),
                                company.getUser(),
                                JobWorkflowStepActivityType.ATTACHMENT_DELETED,
                                "Deleted " + attachment.getFileName());
        }

        @Override
        @Transactional(readOnly = true)
        public List<StepAttachmentResponse> getAttachments(Long stepId, Long companyId) {

                getStep(stepId, companyId);

                return attachmentRepository.findByStepIdOrderByCreatedAtAsc(stepId)
                                .stream()
                                .map(this::map)
                                .toList();
        }

        @Override
        @Transactional(readOnly = true)
        public List<StepTimelineItemResponse> getCommentsAndAttachmentsTimeline(
                        Long stepId,
                        Long companyId) {

                getStep(stepId, companyId);

                return StepTimelineBuilder.build(
                                commentRepository.findByStepIdOrderByCreatedAtAsc(stepId),
                                attachmentRepository.findByStepIdOrderByCreatedAtAsc(stepId),
                                s3Service::resolveFileUrl);
        }

        /*
         * ===========================
         * TIMELINE
         * ===========================
         */

        @Override
        @Transactional(readOnly = true)
        public List<StepActivityResponse> getTimeline(Long stepId, Long companyId) {

                getStep(stepId, companyId);

                return activityRepository.findByStepIdOrderByCreatedAtAsc(stepId)
                                .stream()
                                .map(this::map)
                                .toList();
        }

        /*
         * ===========================
         * MAPPERS
         * ===========================
         */

        private StepCommentResponse map(JobWorkflowStepComment c) {
                return StepCommentResponse.builder()
                                .id(c.getId())
                                .content(c.getContent())
                                .type(c.getType())
                                .authorId(c.getAuthor().getId())
                                .authorUsername(c.getAuthor().getUsername())
                                .createdAt(c.getCreatedAt())
                                .updatedAt(c.getUpdatedAt())
                                .build();
        }

        private StepAttachmentResponse map(JobWorkflowStepAttachment a) {
                return StepAttachmentResponse.builder()
                                .id(a.getId())
                                .fileName(a.getFileName())
                                .fileType(a.getFileType())
                                .fileUrl(s3Service.resolveFileUrl(a.getFileUrl()))
                                .description(a.getDescription())
                                .type(a.getType())
                                .uploadedBy(a.getUploadedBy().getId())
                                .uploadedByUsername(a.getUploadedBy().getUsername())
                                .createdAt(a.getCreatedAt())
                                .build();
        }

        private StepActivityResponse map(JobWorkflowStepActivity a) {
                return StepActivityResponse.builder()
                                .id(a.getId())
                                .type(a.getType().name())
                                .message(a.getMessage())
                                .actorId(a.getActor().getId())
                                .actorUsername(a.getActor().getUsername())
                                .createdAt(a.getCreatedAt())
                                .build();
        }
}
