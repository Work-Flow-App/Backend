package com.workflow.dto.estimate;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstimateUpdateRequest {
    private String notes;
}
