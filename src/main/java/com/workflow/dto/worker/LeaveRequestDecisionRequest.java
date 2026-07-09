package com.workflow.dto.worker;

import jakarta.validation.constraints.Size;

public record LeaveRequestDecisionRequest(
        @Size(max = 500, message = "Decision note cannot exceed 500 characters")
        String decisionNote
) {}
