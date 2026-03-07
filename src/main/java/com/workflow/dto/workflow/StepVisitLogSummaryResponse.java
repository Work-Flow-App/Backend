package com.workflow.dto.workflow;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class StepVisitLogSummaryResponse {
    private List<StepVisitLogResponse> visitLogs;
    private Long totalWorkedMinutes; // Total time across all logs
}