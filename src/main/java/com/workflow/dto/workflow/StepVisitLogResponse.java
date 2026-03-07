package com.workflow.dto.workflow;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
public class StepVisitLogResponse {
    private Long id;
    private LocalDate visitDate;
    private LocalTime timeIn;
    private LocalTime timeOut;
    private Long workedMinutes;
    private String description;
    private Long loggedById;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}