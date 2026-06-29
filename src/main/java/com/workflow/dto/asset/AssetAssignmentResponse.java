package com.workflow.dto.asset;

import lombok.*;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.workflow.common.constant.asset.AssetLocationType;
import com.workflow.dto.job.AddressResponse;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetAssignmentResponse {
    private Long assignmentId;
    private Long assetId;
    private Long jobId;
    private Long assignedWorkerId;

    private String assetName;
    private String description;
    private String serialNumber;
    private String assetTag;

    private String notes;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private LocalDateTime assignedAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private LocalDateTime returnedAt;
    private Long durationDays;
    private String status; // ACTIVE or COMPLETED
    private AssetLocationType locationType;
    private AddressResponse address;
    private Integer expectedDurationDays;
    private Boolean slaBreached;
}
