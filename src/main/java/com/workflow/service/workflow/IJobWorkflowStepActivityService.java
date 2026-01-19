package com.workflow.service.workflow;

import java.io.IOException;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.workflow.dto.workflow.StepActivityResponse;
import com.workflow.dto.workflow.StepAttachmentResponse;
import com.workflow.dto.workflow.StepCommentCreateRequest;
import com.workflow.dto.workflow.StepCommentResponse;

public interface IJobWorkflowStepActivityService {

        // ===== COMMENTS =====
        StepCommentResponse addComment(
                        Long stepId,
                        StepCommentCreateRequest request,
                        Long companyId);

        StepCommentResponse updateComment(
                        Long commentId,
                        StepCommentCreateRequest request,
                        Long companyId);

        void deleteComment(
                        Long commentId,
                        Long companyId);

        List<StepCommentResponse> getComments(
                        Long stepId,
                        Long companyId);

        // ===== ATTACHMENTS =====
        StepAttachmentResponse uploadAttachment(
                        Long stepId,
                        MultipartFile file,
                        Long companyId) throws IOException;

        StepAttachmentResponse updateAttachmentName(
                        Long attachmentId,
                        String newFileName,
                        Long companyId);

        void deleteAttachment(
                        Long attachmentId,
                        Long companyId);

        List<StepAttachmentResponse> getAttachments(
                        Long stepId,
                        Long companyId);

        // ===== TIMELINE =====
        List<StepActivityResponse> getTimeline(
                        Long stepId,
                        Long companyId);
}
