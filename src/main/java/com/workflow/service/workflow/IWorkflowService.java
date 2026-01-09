package com.workflow.service.workflow;

import com.workflow.dto.workflow.*;

import java.util.List;

public interface IWorkflowService {

    WorkflowResponse createWorkflow(WorkflowCreateRequest request, Long companyId);

    WorkflowResponse updateWorkflow(Long id, WorkflowCreateRequest request, Long companyId);

    void deleteWorkflow(Long id, Long companyId);

    List<WorkflowResponse> getAllWorkflows(Long companyId);

    WorkflowResponse getWorkflow(Long id, Long companyId);

    WorkflowStepResponse createStep(WorkflowStepCreateRequest request, Long companyId);

    List<WorkflowStepResponse> getSteps(Long workflowId, Long companyId);

    WorkflowStepResponse getStep(Long stepId, Long companyId);

    List<WorkflowStepResponse> getAllSteps(Long companyId);

    WorkflowStepResponse updateStep(Long stepId, WorkflowStepCreateRequest request, Long companyId);

    void deleteStep(Long stepId, Long companyId);

    WorkflowResponse bulkUpdateWorkflow(
            Long workflowId,
            WorkflowBulkUpdateRequest request,
            Long companyId);

    WorkflowWithStepsResponse getWorkflowWithSteps(Long workflowId, Long companyId);

}
