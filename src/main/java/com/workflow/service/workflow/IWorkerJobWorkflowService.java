package com.workflow.service.workflow;

import java.io.IOException;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

import com.workflow.common.constant.workflow.StepDiscussionType;
import com.workflow.dto.workflow.JobWorkflowResponse;
import com.workflow.dto.workflow.StepAttachmentResponse;
import com.workflow.dto.workflow.StepCommentCreateRequest;
import com.workflow.dto.workflow.StepCommentResponse;
import com.workflow.dto.workflow.StepTimelineItemResponse;
import com.workflow.dto.workflow.JobWorkflowStepResponse;
import com.workflow.dto.workflow.StepVisitLogCreateRequest;
import com.workflow.dto.workflow.StepVisitLogResponse;
import com.workflow.dto.workflow.StepVisitLogSummaryResponse;
import com.workflow.dto.workflow.WorkerAssignedStepResponse;

public interface IWorkerJobWorkflowService {
    // Read Operations
    List<WorkerAssignedStepResponse> getMyAssignedSteps(Long workerUserId);

    List<JobWorkflowResponse> getAssignedJobWorkflows(Long workerUserId);

    JobWorkflowResponse getJobWorkflowIfAssigned(Long jobWorkflowId, Long workerUserId);

    JobWorkflowStepResponse getStepIfAssigned(Long stepId, Long workerUserId);

    List<StepCommentResponse> getStepComments(Long stepId, Long workerUserId);

    List<StepAttachmentResponse> getStepAttachments(Long stepId, Long workerUserId);

    List<StepTimelineItemResponse> getStepTimeline(Long stepId, Long workerUserId);

    StepVisitLogSummaryResponse getVisitLogs(Long stepId, Long workerUserId);

    // Status Actions
    JobWorkflowStepResponse startStep(Long stepId, Long workerUserId);

    JobWorkflowStepResponse completeStep(Long stepId, Long workerUserId);

    JobWorkflowStepResponse markStepOngoing(Long stepId, Long workerUserId);

    JobWorkflowStepResponse completeOngoingStep(Long stepId, Long workerUserId);

    // Activities (Comments/Attachments/Visits)
    StepCommentResponse addComment(Long stepId, StepCommentCreateRequest request, Long workerUserId);

    StepAttachmentResponse uploadAttachment(
            Long stepId,
            MultipartFile file,
            StepDiscussionType type,
            String description,
            Long workerUserId) throws IOException;

    StepVisitLogResponse addVisitLog(Long stepId, StepVisitLogCreateRequest request, Long workerUserId);
}