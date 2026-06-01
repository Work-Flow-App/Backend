package com.workflow.dto.workflow;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

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

    private String actorUsername;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private LocalDateTime createdAt;
}