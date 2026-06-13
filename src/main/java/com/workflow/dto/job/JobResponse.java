package com.workflow.dto.job;

import com.workflow.common.constant.job.JobStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobResponse {
    private Long id;
    private Long jobRef;
    private Long companyId;

    private Long templateId;
    private String templateName;

    private Long clientId;
    private String clientName;

    private Long customerId;
    private String customerName;

    private Long workflowId;
    private String workflowName;

    private List<Long> assignedWorkerIds;
    private JobStatus status;
    private boolean archived;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private LocalDateTime updatedAt;

    private Map<Long, FieldValueResponse> fieldValues;
    private List<Long> assetIds;
    private AddressResponse address;
    private Long estimateId;
    private BigDecimal estimateTotalNet;
}