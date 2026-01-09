package com.workflow.dto.workflow;

import java.util.List;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowWithStepsResponse {
    private Long id;
    private Long companyId;
    private String name;
    private String description;
    private List<WorkflowStepResponse> steps;
}
