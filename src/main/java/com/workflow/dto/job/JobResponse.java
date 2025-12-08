package com.workflow.dto.job;

import lombok.*;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobResponse {
    private Long id;
    private Long companyId;
    private Long templateId;
    private Long clientId;
    private Long assignedWorkerId;
    private String status;
    private boolean archived;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Map<Long, FieldValueResponse> fieldValues;
}
