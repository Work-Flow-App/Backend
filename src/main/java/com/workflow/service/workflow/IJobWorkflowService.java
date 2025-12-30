package com.workflow.service.workflow;

import java.util.List;

import com.workflow.dto.workflow.*;
import com.workflow.entity.*;

public interface IJobWorkflowService {
    JobWorkflowResponse startWorkflow(Job job, Workflow workflow);

    JobWorkflowResponse getJobWorkflow(Job job);

    JobWorkflowStepResponse updateStep(Long jobId, Long stepId, JobWorkflowStepUpdateRequest request);

    JobWorkflowResponse getJobWorkflowById(Long jobWorkflowId, Long companyId);

    List<JobWorkflowResponse> getAllJobWorkflows(Long companyId);

    void deleteByJobId(Long jobId);

}
