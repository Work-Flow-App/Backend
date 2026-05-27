package com.workflow.dto.estimate;

import com.workflow.common.constant.financial.LineItemStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LineItemStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private LineItemStatus status;
}
