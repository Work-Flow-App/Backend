package com.workflow.dto.workflow;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StepTimelineItemResponse {

    private Long id;
    private String itemType; // COMMENT | ATTACHMENT
    private String content; // comment content OR attachment name
    private String fileUrl; // only for attachment
    private Long actorId; // author or uploader
    private LocalDateTime createdAt;
}
