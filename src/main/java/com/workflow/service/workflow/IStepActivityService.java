package com.workflow.service.workflow;

import java.util.List;

import com.workflow.common.constant.workflow.JobWorkflowStepActivityType;
import com.workflow.dto.workflow.StepActivityResponse;
import com.workflow.entity.job.JobWorkflowStep;
import com.workflow.entity.auth.User;

public interface IStepActivityService {

    void log(
            JobWorkflowStep step,
            User actor,
            JobWorkflowStepActivityType type,
            String message);

    /**
     * Batch-save activity log entries for multiple steps in a single INSERT.
     * Use this in place of calling {@link #log} in a loop.
     */
    void logAll(
            List<JobWorkflowStep> steps,
            User actor,
            JobWorkflowStepActivityType type,
            String message);

    // ===== TIMELINE =====
    List<StepActivityResponse> getTimeline(
            Long stepId,
            Long companyId);
}
