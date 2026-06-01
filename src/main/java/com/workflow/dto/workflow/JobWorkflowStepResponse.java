package com.workflow.dto.workflow;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Set;

import com.workflow.common.constant.workflow.SlaStatus;
import com.workflow.common.constant.workflow.WorkflowStepStatus;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobWorkflowStepResponse {

    private Long id;
    private String name;
    private String description;
    private Integer orderIndex;
    private WorkflowStepStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private LocalDateTime startedAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private LocalDateTime completedAt;

    private Integer expectedDurationMinutes;
    private Integer maximumDurationMinutes;
    private SlaStatus slaStatus; // Dynamically calculated

    private Set<Long> assignedWorkerIds;
}