package com.workflow.dto.workflow;

import java.time.LocalDateTime;

import lombok.*;

@Data
@Builder
public class StepActivityResponse {
    private Long id;
    private String type;
    private String message;
    private Long actorId;
    private String actorUsername;
    private LocalDateTime createdAt;
}
