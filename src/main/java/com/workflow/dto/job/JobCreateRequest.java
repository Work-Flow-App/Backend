package com.workflow.dto.job;

import java.util.List;
import java.util.Map;

import com.workflow.common.constant.job.JobStatus;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobCreateRequest {
    private Long templateId;
    private Long clientId;

    @NotNull(message = "Customer is required")
    private Long customerId;
    private Long assignedWorkerId;
    private Long workflowId;
    private JobStatus status;
    // key = fieldId, value = actual object (String, Number, Boolean, Date, Map,
    // Reference)
    private Map<Long, Object> fieldValues;
    private List<Long> assetIds;
    private AddressRequest address;
}
