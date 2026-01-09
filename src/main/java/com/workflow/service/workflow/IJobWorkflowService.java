package com.workflow.service.workflow;

import java.util.List;

import com.workflow.dto.workflow.JobWorkflowResponse;
import com.workflow.dto.workflow.JobWorkflowStepResponse;
import com.workflow.dto.workflow.JobWorkflowStepUpdateRequest;
import com.workflow.dto.workflow.JobWorkflowUpdateRequest;
import com.workflow.entity.Job;
import com.workflow.entity.Workflow;

public interface IJobWorkflowService {
    JobWorkflowResponse startWorkflow(Job job, Workflow workflow, Long companyId);

    JobWorkflowResponse getJobWorkflow(Job job, Long companyId);

    JobWorkflowStepResponse updateStep(
            Long jobId,
            Long stepId,
            JobWorkflowStepUpdateRequest request,
            Long companyId);

    JobWorkflowResponse getJobWorkflowById(Long jobWorkflowId, Long companyId);

    List<JobWorkflowResponse> getAllJobWorkflows(Long companyId);

    void deleteByJobId(Long jobId, Long companyId);

    JobWorkflowResponse assignAWorkerToAllSteps(Long jobWorkflowId, Long workerId, Long companyId);

    JobWorkflowResponse updateJobWorkflowById(Long jobWorkflowId, JobWorkflowUpdateRequest request, Long companyId);

    /* JobWorkflowStep addStep(Long jobWorkflowId, JobWorkflowStepCreateRequest request); */

}
