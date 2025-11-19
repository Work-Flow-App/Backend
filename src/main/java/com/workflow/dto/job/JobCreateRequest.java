package com.workflow.dto.job;

import lombok.*;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobCreateRequest {
    private Long templateId;
    private Long clientId;
    private Long assignedWorkerId;
    private String status;
    private Map<Long, String> fieldValues; // key = fieldId, value = value
}
