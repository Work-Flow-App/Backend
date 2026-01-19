package com.workflow.service.workflow;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.workflow.common.constant.workflow.JobWorkflowStepActivityType;
import com.workflow.common.exception.business.AttachmentNotFoundException;
import com.workflow.common.exception.business.CommentNotFoundException;
import com.workflow.common.exception.business.ForbiddenActionException;
import com.workflow.common.exception.business.UnauthorizedWorkflowAccessException;
import com.workflow.dto.workflow.StepActivityResponse;
import com.workflow.dto.workflow.StepAttachmentResponse;
import com.workflow.dto.workflow.StepCommentCreateRequest;
import com.workflow.dto.workflow.StepCommentResponse;
import com.workflow.entity.Company;
import com.workflow.entity.JobWorkflowStep;
import com.workflow.entity.JobWorkflowStepActivity;
import com.workflow.entity.JobWorkflowStepAttachment;
import com.workflow.entity.JobWorkflowStepComment;
import com.workflow.repository.CompanyRepository;
import com.workflow.repository.JobWorkflowStepActivityRepository;
import com.workflow.repository.JobWorkflowStepAttachmentRepository;
import com.workflow.repository.JobWorkflowStepCommentRepository;
import com.workflow.repository.JobWorkflowStepRepository;
import com.workflow.service.storage.S3StorageService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class JobWorkflowStepActivityService
                implements IJobWorkflowStepActivityService {

        private final JobWorkflowStepRepository stepRepository;
        private final JobWorkflowStepCommentRepository commentRepository;
        private final JobWorkflowStepAttachmentRepository attachmentRepository;
        private final JobWorkflowStepActivityRepository activityRepository;
        private final CompanyRepository companyRepository;
        private final S3StorageService s3Service;

        /*
         * ===========================
         * INTERNAL HELPERS
         * ===========================
         */

        private Company getCompany(Long companyId) {
                return companyRepository.findById(companyId)
                                .orElseThrow(() -> new IllegalStateException("Company not found"));
        }

        private JobWorkflowStep getStep(Long stepId, Long companyId) {
                JobWorkflowStep step = stepRepository.findById(stepId)
                                .orElseThrow(() -> new IllegalStateException("Step not found"));

                if (!step.getJobWorkflow()
                                .getJob()
                                .getCompany()
                                .getId()
                                .equals(companyId)) {
                        throw new UnauthorizedWorkflowAccessException("Unauthorized access");
                }
                return step;
        }

        private void logActivity(
                        JobWorkflowStep step,
                        Company company,
                        JobWorkflowStepActivityType type,
                        String message) {

                activityRepository.save(
                                JobWorkflowStepActivity.builder()
                                                .step(step)
                                                .actor(company.getUser())
                                                .type(type)
                                                .message(message)
                                                .build());
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
                                                .build());

                logActivity(step, company, JobWorkflowStepActivityType.COMMENT, request.getContent());

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

                comment.setContent(request.getContent());

                logActivity(
                                comment.getStep(),
                                company,
                                JobWorkflowStepActivityType.COMMENT,
                                "Edited a comment");

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

                logActivity(
                                comment.getStep(),
                                company,
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
                        Long companyId) throws IOException {

                Company company = getCompany(companyId);
                JobWorkflowStep step = getStep(stepId, companyId);

                // Build S3 key (you can adjust the structure)
                String key = String.format(
                                "companies/%d/steps/%d/%s",
                                companyId,
                                stepId,
                                file.getOriginalFilename());

                String url = s3Service.upload(
                                key,
                                file.getInputStream(),
                                file.getSize(),
                                file.getContentType());

                JobWorkflowStepAttachment attachment = attachmentRepository.save(
                                JobWorkflowStepAttachment.builder()
                                                .step(step)
                                                .uploadedBy(company.getUser())
                                                .fileName(file.getOriginalFilename())
                                                .fileType(file.getContentType())
                                                .fileUrl(url)
                                                .build());

                logActivity(
                                step,
                                company,
                                JobWorkflowStepActivityType.ATTACHMENT_ADDED,
                                "Uploaded " + file.getOriginalFilename());

                return map(attachment);
        }

        @Override
        public StepAttachmentResponse updateAttachmentName(
                        Long attachmentId,
                        String newFileName,
                        Long companyId) {

                Company company = getCompany(companyId);

                JobWorkflowStepAttachment attachment = attachmentRepository.findById(attachmentId)
                                .orElseThrow(() -> new AttachmentNotFoundException("Attachment not found"));

                if (!attachment.getUploadedBy().getId().equals(company.getUser().getId())) {
                        throw new ForbiddenActionException("Not allowed to rename this attachment");
                }

                attachment.setFileName(newFileName);

                logActivity(
                                attachment.getStep(),
                                company,
                                JobWorkflowStepActivityType.ATTACHMENT_UPDATED,
                                "Renamed attachment to " + newFileName);

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
                attachmentRepository.delete(attachment);

                logActivity(
                                attachment.getStep(),
                                company,
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
                                .authorId(c.getAuthor().getId())
                                .createdAt(c.getCreatedAt())
                                .updatedAt(c.getUpdatedAt())
                                .build();
        }

        private StepAttachmentResponse map(JobWorkflowStepAttachment a) {
                return StepAttachmentResponse.builder()
                                .id(a.getId())
                                .fileName(a.getFileName())
                                .fileType(a.getFileType())
                                .fileUrl(a.getFileUrl())
                                .uploadedBy(a.getUploadedBy().getId())
                                .createdAt(a.getCreatedAt())
                                .build();
        }

        private StepActivityResponse map(JobWorkflowStepActivity a) {
                return StepActivityResponse.builder()
                                .id(a.getId())
                                .type(a.getType().name())
                                .message(a.getMessage())
                                .actorId(a.getActor().getId())
                                .createdAt(a.getCreatedAt())
                                .build();
        }
}
