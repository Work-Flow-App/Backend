package com.workflow.dto.workflow;

import java.time.LocalDateTime;

import com.workflow.common.constant.workflow.StepDiscussionType;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StepTimelineItemResponse {

    private Long id;

    private String itemType; // COMMENT | ATTACHMENT

    private String content; // comment content OR attachment name

    private String fileUrl; // only for attachment

    private StepDiscussionType discussionType; // Type of discussion (e.g., GENERAL, ISSUE, etc.)

    private String description; // only for attachment

    private Long actorId;

    private LocalDateTime createdAt;
}