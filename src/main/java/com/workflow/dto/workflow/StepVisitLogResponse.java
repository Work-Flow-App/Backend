package com.workflow.dto.workflow;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

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
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private LocalDateTime updatedAt;
}