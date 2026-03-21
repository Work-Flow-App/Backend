package com.workflow.dto.workflow;

import com.workflow.dto.asset.AssetAssignmentResponse;
import com.workflow.dto.customer.CustomerResponse;
import com.workflow.dto.job.AddressResponse;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkerAssignedStepResponse {
    private JobWorkflowStepResponse step;
    private Long jobId;
    private CustomerResponse customer;
    private List<AssetAssignmentResponse> assignedAssets;
    private AddressResponse jobAddress;
}