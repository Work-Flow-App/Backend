package com.workflow.dto.job;

import lombok.*;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobUpdateRequest {
    private Long clientId;
    private Long assignedWorkerId;
    private String status;
    private boolean archived;
    private Map<Long, String> fieldValues;
}
