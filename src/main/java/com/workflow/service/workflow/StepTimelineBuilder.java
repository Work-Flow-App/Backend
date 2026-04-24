package com.workflow.service.workflow;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import com.workflow.dto.workflow.StepTimelineItemResponse;
import com.workflow.entity.job.JobWorkflowStepAttachment;
import com.workflow.entity.job.JobWorkflowStepComment;

/**
 * Package-private helper that merges comment and attachment lists into a single
 * chronologically sorted timeline. Extracted to eliminate duplicated logic in
 * {@link JobWorkflowStepActivityService} and {@link WorkerJobWorkflowService}.
 */
class StepTimelineBuilder {

    private StepTimelineBuilder() {
    }

    /**
     * Merges already-fetched comment and attachment lists into a single timeline
     * sorted by {@code createdAt} ascending.
     *
     * @param comments    ordered list of comments for the step
     * @param attachments ordered list of attachments for the step
     * @param urlResolver function that converts a storage key to a presigned URL
     *                    (may return null for null keys)
     * @return merged, sorted timeline
     */
    static List<StepTimelineItemResponse> build(
            List<JobWorkflowStepComment> comments,
            List<JobWorkflowStepAttachment> attachments,
            Function<String, String> urlResolver) {

        List<StepTimelineItemResponse> commentItems = comments.stream()
                .map(c -> StepTimelineItemResponse.builder()
                        .id(c.getId())
                        .itemType("COMMENT")
                        .content(c.getContent())
                        .discussionType(c.getType())
                        .actorId(c.getAuthor().getId())
                        .createdAt(c.getCreatedAt())
                        .build())
                .toList();

        List<StepTimelineItemResponse> attachmentItems = attachments.stream()
                .map(a -> StepTimelineItemResponse.builder()
                        .id(a.getId())
                        .itemType("ATTACHMENT")
                        .content(a.getFileName())
                        .fileUrl(urlResolver.apply(a.getFileUrl()))
                        .discussionType(a.getType())
                        .description(a.getDescription())
                        .actorId(a.getUploadedBy().getId())
                        .createdAt(a.getCreatedAt())
                        .build())
                .toList();

        return Stream.concat(commentItems.stream(), attachmentItems.stream())
                .sorted(Comparator.comparing(StepTimelineItemResponse::getCreatedAt))
                .toList();
    }
}