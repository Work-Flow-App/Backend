package com.workflow.dto.workflow;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowResponse {
    private Long id;
    private Long companyId;
    private String name;
    private String description;
}
