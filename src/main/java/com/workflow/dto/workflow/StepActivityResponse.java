package com.workflow.dto.workflow;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.*;

@Data
@Builder
public class StepActivityResponse {
    private Long id;
    private String type;
    private String message;
    private Long actorId;
    private String actorUsername;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private LocalDateTime createdAt;
}
