package com.workflow.controller.workflow;

import java.io.IOException;
import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.workflow.common.constant.workflow.StepDiscussionType;
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
import com.workflow.entity.auth.User;
import com.workflow.service.workflow.IWorkerJobWorkflowService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Worker Job Workflows", description = "Endpoints for workers to manage their assigned workflows and steps")
@RestController
@RequestMapping("/api/v1/worker")
@RequiredArgsConstructor
public class WorkerJobWorkflowController {

    private final IWorkerJobWorkflowService workerWorkflowService;

    // Helper to get authenticated User ID
    private Long getUserId(Authentication auth) {
        User user = (User) auth.getPrincipal();
        return user.getId();
    }

    // ==========================================
    // WORKFLOW READ OPERATIONS
    // ==========================================

    @Operation(summary = "Get all job workflows assigned to the current worker")
    @GetMapping("/job-workflows")
    public ResponseEntity<List<JobWorkflowResponse>> getMyJobWorkflows(Authentication auth) {
        return ResponseEntity.ok(workerWorkflowService.getAssignedJobWorkflows(getUserId(auth)));
    }

    @Operation(summary = "Get specific job workflow details if assigned")
    @GetMapping("/job-workflows/{jobWorkflowId}")
    public ResponseEntity<JobWorkflowResponse> getJobWorkflow(
            @PathVariable Long jobWorkflowId,
            Authentication auth) {
        return ResponseEntity.ok(workerWorkflowService.getJobWorkflowIfAssigned(jobWorkflowId, getUserId(auth)));
    }

    @Operation(summary = "Get all visit logs for a step")
    @GetMapping("/job-workflow-steps/{stepId}/visits")
    public ResponseEntity<StepVisitLogSummaryResponse> getStepVisits(
            @PathVariable Long stepId,
            Authentication auth) {

        // Calls the service method which verifies the worker is assigned to the step
        // before returning the summary of visit logs ordered by date/time.
        return ResponseEntity.ok(workerWorkflowService.getVisitLogs(stepId, getUserId(auth)));
    }

    // ==========================================
    // STEP ACTIONS
    // ==========================================

    @Operation(summary = "Start a step (NOT_STARTED -> STARTED)")
    @PostMapping("/job-workflow-steps/{stepId}/start")
    public ResponseEntity<JobWorkflowStepResponse> startStep(
            @PathVariable Long stepId,
            Authentication auth) {
        return ResponseEntity.ok(workerWorkflowService.startStep(stepId, getUserId(auth)));
    }

    @Operation(summary = "Complete a step (STARTED -> COMPLETED)")
    @PostMapping("/job-workflow-steps/{stepId}/complete")
    public ResponseEntity<JobWorkflowStepResponse> completeStep(
            @PathVariable Long stepId,
            Authentication auth) {
        return ResponseEntity.ok(workerWorkflowService.completeStep(stepId, getUserId(auth)));
    }

    @Operation(summary = "Mark an initiated step as ongoing (INITIATED -> ONGOING)")
    @PostMapping("/job-workflow-steps/{stepId}/ongoing")
    public ResponseEntity<JobWorkflowStepResponse> markStepOngoing(
            @PathVariable Long stepId,
            Authentication auth) {
        return ResponseEntity.ok(workerWorkflowService.markStepOngoing(stepId, getUserId(auth)));
    }

    @Operation(summary = "Complete an ongoing step (ONGOING -> COMPLETED)")
    @PostMapping("/job-workflow-steps/{stepId}/complete-ongoing")
    public ResponseEntity<JobWorkflowStepResponse> completeOngoingStep(
            @PathVariable Long stepId,
            Authentication auth) {
        return ResponseEntity.ok(workerWorkflowService.completeOngoingStep(stepId, getUserId(auth)));
    }

    @Operation(summary = "Get step details")
    @GetMapping("/job-workflow-steps/{stepId}")
    public ResponseEntity<JobWorkflowStepResponse> getStep(
            @PathVariable Long stepId,
            Authentication auth) {
        return ResponseEntity.ok(workerWorkflowService.getStepIfAssigned(stepId, getUserId(auth)));
    }

    // ==========================================
    // STEP ACTIVITIES (Comments, Attachments, Timeline)
    // ==========================================

    @Operation(summary = "Get all comments for a step")
    @GetMapping("/job-workflow-steps/{stepId}/comments")
    public ResponseEntity<List<StepCommentResponse>> getStepComments(
            @PathVariable Long stepId,
            Authentication auth) {

        return ResponseEntity.ok(
                workerWorkflowService.getStepComments(stepId, getUserId(auth)));
    }

    @Operation(summary = "Get all attachments for a step")
    @GetMapping("/job-workflow-steps/{stepId}/attachments")
    public ResponseEntity<List<StepAttachmentResponse>> getStepAttachments(
            @PathVariable Long stepId,
            Authentication auth) {

        return ResponseEntity.ok(
                workerWorkflowService.getStepAttachments(stepId, getUserId(auth)));
    }

    @Operation(summary = "Get discussion timeline (Comments & Attachments) for a step")
    @GetMapping("/job-workflow-steps/{stepId}/discussion")
    public ResponseEntity<List<StepTimelineItemResponse>> getStepDiscussion(
            @PathVariable Long stepId,
            Authentication auth) {
        return ResponseEntity.ok(workerWorkflowService.getStepTimeline(stepId, getUserId(auth)));
    }

    @Operation(summary = "Add a comment to a step")
    @PostMapping("/job-workflow-steps/{stepId}/comments")
    public ResponseEntity<StepCommentResponse> addComment(
            @PathVariable Long stepId,
            @Valid @RequestBody StepCommentCreateRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workerWorkflowService.addComment(stepId, request, getUserId(auth)));
    }

    @Operation(summary = "Upload an attachment to a step")
    @PostMapping(value = "/job-workflow-steps/{stepId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StepAttachmentResponse> uploadAttachment(
            @PathVariable Long stepId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") StepDiscussionType type,
            @RequestParam(value = "description", required = false) String description, Authentication auth)
            throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workerWorkflowService.uploadAttachment(
                        stepId,
                        file,
                        type,
                        description,
                        getUserId(auth)));

    }

    @Operation(summary = "Get all job workflow steps assigned to the current worker")
    @GetMapping("/job-workflow-steps")
    public ResponseEntity<List<WorkerAssignedStepResponse>> getMyAssignedSteps(
            Authentication auth) {

        return ResponseEntity.ok(
                workerWorkflowService.getMyAssignedSteps(getUserId(auth)));
    }

    @Operation(summary = "Add a visit log to a step")
    @PostMapping("/job-workflow-steps/{stepId}/visits")
    public ResponseEntity<StepVisitLogResponse> addVisitLog(
            @PathVariable Long stepId,
            @Valid @RequestBody StepVisitLogCreateRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workerWorkflowService.addVisitLog(stepId, request, getUserId(auth)));
    }

}