package com.workflow.service.workflow;

import com.workflow.dto.workflow.*;
import com.workflow.entity.*;

public interface IJobWorkflowService {
    JobWorkflowResponse startWorkflow(Job job, Workflow workflow);

    JobWorkflowResponse getJobWorkflow(Job job);

    JobWorkflowStepResponse updateStep(Long jobId, Long stepId, JobWorkflowStepUpdateRequest request);

    void deleteByJobId(Long jobId);

}
