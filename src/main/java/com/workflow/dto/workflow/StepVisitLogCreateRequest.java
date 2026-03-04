package com.workflow.dto.workflow;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class StepVisitLogCreateRequest {
    private LocalDate visitDate;
    private LocalTime timeIn;
    private LocalTime timeOut;
    private String description;
}