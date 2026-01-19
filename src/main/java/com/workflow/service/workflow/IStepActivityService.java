package com.workflow.service.workflow;

import com.workflow.common.constant.workflow.JobWorkflowStepActivityType;
import com.workflow.entity.JobWorkflowStep;
import com.workflow.entity.User;

public interface IStepActivityService {

    void log(
            JobWorkflowStep step,
            User actor,
            JobWorkflowStepActivityType type,
            String message);
}
