package com.workflow.controller.workflow;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.workflow.common.constant.workflow.StepDiscussionType;
import com.workflow.common.util.AuthUtils;
import com.workflow.dto.workflow.StepAttachmentResponse;
import com.workflow.dto.workflow.StepAttachmentUpdateRequest;
import com.workflow.dto.workflow.StepCommentCreateRequest;
import com.workflow.dto.workflow.StepCommentResponse;
import com.workflow.dto.workflow.StepTimelineItemResponse;
import com.workflow.service.company.ICompanyService;
import com.workflow.service.workflow.IJobWorkflowStepActivityService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Workflow Step Activities")
@RestController
@RequestMapping("/api/v1/job-workflow-steps")
@RequiredArgsConstructor
public class JobWorkflowStepActivityController {

        private final IJobWorkflowStepActivityService stepActivityService;
        private final ICompanyService companyService;

        /*
         * ===========================
         * HELPERS
         * ===========================
         */

        private Long getCompanyId(Authentication auth) {
                return AuthUtils.getCompanyId(auth, companyService);
        }

        /*
         * ===========================
         * COMMENTS
         * ===========================
         */

        @PostMapping("/{stepId}/comments")
        public ResponseEntity<StepCommentResponse> addComment(
                        @PathVariable Long stepId,
                        @RequestBody StepCommentCreateRequest request,
                        Authentication auth) {

                StepCommentResponse response = stepActivityService.addComment(
                                stepId,
                                request,
                                getCompanyId(auth));

                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        @PutMapping("/comments/{commentId}")
        public ResponseEntity<StepCommentResponse> updateComment(
                        @PathVariable Long commentId,
                        @RequestBody StepCommentCreateRequest request,
                        Authentication auth) {

                return ResponseEntity.ok(
                                stepActivityService.updateComment(
                                                commentId,
                                                request,
                                                getCompanyId(auth)));
        }

        @DeleteMapping("/comments/{commentId}")
        public ResponseEntity<Void> deleteComment(
                        @PathVariable Long commentId,
                        Authentication auth) {

                stepActivityService.deleteComment(commentId, getCompanyId(auth));
                return ResponseEntity.noContent().build();
        }

        @GetMapping("/{stepId}/comments")
        public ResponseEntity<List<StepCommentResponse>> getComments(
                        @PathVariable Long stepId,
                        Authentication auth) {

                return ResponseEntity.ok(
                                stepActivityService.getComments(stepId, getCompanyId(auth)));
        }

        /*
         * ===========================
         * ATTACHMENTS
         * ===========================
         */

        @PostMapping(value = "/{stepId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<StepAttachmentResponse> uploadAttachment(
                        @PathVariable Long stepId,
                        @RequestParam("file") MultipartFile file,
                        @RequestParam("type") StepDiscussionType type,
                        @RequestParam(value = "description", required = false) String description,
                        Authentication auth) throws IOException {

                StepAttachmentResponse response = stepActivityService.uploadAttachment(
                                stepId,
                                file,
                                type,
                                description,
                                getCompanyId(auth));

                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        @PutMapping("/attachments/{attachmentId}")
        public ResponseEntity<StepAttachmentResponse> updateAttachment(
                        @PathVariable Long attachmentId,
                        @RequestBody StepAttachmentUpdateRequest request,
                        Authentication auth) {

                return ResponseEntity.ok(
                                stepActivityService.updateAttachment(
                                                attachmentId,
                                                request,
                                                getCompanyId(auth)));
        }

        @DeleteMapping("/attachments/{attachmentId}")
        public ResponseEntity<Void> deleteAttachment(
                        @PathVariable Long attachmentId,
                        Authentication auth) {

                stepActivityService.deleteAttachment(
                                attachmentId,
                                getCompanyId(auth));

                return ResponseEntity.noContent().build();
        }

        @GetMapping("/{stepId}/attachments")
        public ResponseEntity<List<StepAttachmentResponse>> getAttachments(
                        @PathVariable Long stepId,
                        Authentication auth) {

                return ResponseEntity.ok(
                                stepActivityService.getAttachments(stepId, getCompanyId(auth)));
        }

        @GetMapping("/{stepId}/discussion")
        public ResponseEntity<List<StepTimelineItemResponse>> getDiscussionTimeline(
                        @PathVariable Long stepId,
                        Authentication auth) {

                return ResponseEntity.ok(
                                stepActivityService.getCommentsAndAttachmentsTimeline(
                                                stepId,
                                                getCompanyId(auth)));
        }

}
