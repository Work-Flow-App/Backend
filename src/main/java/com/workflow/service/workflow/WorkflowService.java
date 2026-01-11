package com.workflow.service.workflow;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workflow.common.exception.business.UnauthorizedWorkflowAccessException;
import com.workflow.common.exception.business.WorkflowNotFoundException;
import com.workflow.common.exception.business.WorkflowStepNotFoundException;
import com.workflow.dto.workflow.WorkflowBulkUpdateRequest;
import com.workflow.dto.workflow.WorkflowCreateRequest;
import com.workflow.dto.workflow.WorkflowResponse;
import com.workflow.dto.workflow.WorkflowStepBulkRequest;
import com.workflow.dto.workflow.WorkflowStepCreateRequest;
import com.workflow.dto.workflow.WorkflowStepResponse;
import com.workflow.dto.workflow.WorkflowWithStepsResponse;
import com.workflow.entity.Company;
import com.workflow.entity.Workflow;
import com.workflow.entity.WorkflowStep;
import com.workflow.repository.CompanyRepository;
import com.workflow.repository.WorkflowRepository;
import com.workflow.repository.WorkflowStepRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkflowService implements IWorkflowService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowStepRepository stepRepository;
    private final CompanyRepository companyRepository;

    @Override
    public WorkflowResponse createWorkflow(WorkflowCreateRequest request, Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new UnauthorizedWorkflowAccessException("Company not found"));

        Workflow workflow = workflowRepository.save(
                Workflow.builder()
                        .company(company)
                        .name(request.getName())
                        .description(request.getDescription())
                        .build());

        return map(workflow);
    }

    @Override
    public WorkflowResponse updateWorkflow(Long id, WorkflowCreateRequest request, Long companyId) {
        Workflow workflow = workflowRepository.findById(id)
                .filter(w -> w.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow not found"));

        workflow.setName(request.getName());
        workflow.setDescription(request.getDescription());

        return map(workflow);
    }

    @Override
    public void deleteWorkflow(Long id, Long companyId) {
        Workflow workflow = workflowRepository.findById(id)
                .filter(w -> w.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow not found"));

        workflowRepository.delete(workflow);
    }

    @Override
    public List<WorkflowResponse> getAllWorkflows(Long companyId) {
        return workflowRepository.findByCompanyId(companyId)
                .stream().map(this::map).collect(Collectors.toList());
    }

    @Override
    public WorkflowResponse getWorkflow(Long id, Long companyId) {
        Workflow workflow = workflowRepository.findById(id)
                .filter(w -> w.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow not found"));

        return map(workflow);
    }

    @Override
    public WorkflowStepResponse createStep(WorkflowStepCreateRequest request, Long companyId) {
        Workflow workflow = workflowRepository.findById(request.getWorkflowId())
                .filter(w -> w.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow not found"));

        WorkflowStep step = stepRepository.save(
                WorkflowStep.builder()
                        .workflow(workflow)
                        .name(request.getName())
                        .description(request.getDescription())
                        .orderIndex(request.getOrderIndex())
                        .optional(request.isOptional())
                        .build());

        return map(step);
    }

    @Override
    public List<WorkflowStepResponse> getSteps(Long workflowId, Long companyId) {

        Workflow workflow = workflowRepository.findById(workflowId)
                .filter(w -> w.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new UnauthorizedWorkflowAccessException("Unauthorized access"));

        return stepRepository.findByWorkflowIdOrderByOrderIndexAsc(workflow.getId())
                .stream()
                .map(this::map)
                .collect(Collectors.toList());
    }

    @Override
    public WorkflowStepResponse updateStep(Long stepId, WorkflowStepCreateRequest request, Long companyId) {
        WorkflowStep step = stepRepository.findById(stepId)
                .filter(s -> s.getWorkflow().getCompany().getId().equals(companyId))
                .orElseThrow(() -> new WorkflowStepNotFoundException("Workflow step not found"));

        step.setName(request.getName());
        step.setDescription(request.getDescription());
        step.setOrderIndex(request.getOrderIndex());
        step.setOptional(request.isOptional());

        return map(step);
    }

    @Override
    public WorkflowStepResponse getStep(Long stepId, Long companyId) {
        WorkflowStep step = stepRepository.findById(stepId)
                .filter(s -> s.getWorkflow().getCompany().getId().equals(companyId))
                .orElseThrow(() -> new WorkflowStepNotFoundException("Workflow step not found"));

        return map(step);
    }

    @Override
    public List<WorkflowStepResponse> getAllSteps(Long companyId) {
        return stepRepository
                .findByWorkflow_Company_IdOrderByWorkflow_IdAscOrderIndexAsc(companyId)
                .stream()
                .map(this::map)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteStep(Long stepId, Long companyId) {
        WorkflowStep step = stepRepository.findById(stepId)
                .filter(s -> s.getWorkflow().getCompany().getId().equals(companyId))
                .orElseThrow(() -> new WorkflowStepNotFoundException("Workflow step not found"));

        stepRepository.delete(step);
    }

    @Override
    public WorkflowResponse bulkUpdateWorkflow(
            Long workflowId,
            WorkflowBulkUpdateRequest request,
            Long companyId) {

        Workflow workflow = workflowRepository.findById(workflowId)
                .filter(w -> w.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow not found"));

        // 1️⃣ Update workflow fields
        if (request.getName() != null) {
            workflow.setName(request.getName());
        }
        if (request.getDescription() != null) {
            workflow.setDescription(request.getDescription());
        }

        // 2️⃣ Handle steps
        List<WorkflowStep> existingSteps = stepRepository.findByWorkflowIdOrderByOrderIndexAsc(workflowId);

        Map<Long, WorkflowStep> existingMap = existingSteps.stream()
                .collect(Collectors.toMap(WorkflowStep::getId, s -> s));

        Set<Long> incomingIds = new HashSet<>();

        for (WorkflowStepBulkRequest stepReq : request.getSteps()) {

            // 🔹 UPDATE
            if (stepReq.getId() != null) {
                WorkflowStep step = existingMap.get(stepReq.getId());
                if (step == null) {
                    throw new WorkflowStepNotFoundException(
                            "Invalid workflow step id: " + stepReq.getId());

                }

                step.setName(stepReq.getName());
                step.setDescription(stepReq.getDescription());
                step.setOrderIndex(stepReq.getOrderIndex());
                step.setOptional(stepReq.isOptional());

                incomingIds.add(step.getId());
            }

            // 🔹 CREATE
            else {
                stepRepository.save(
                        WorkflowStep.builder()
                                .workflow(workflow)
                                .name(stepReq.getName())
                                .description(stepReq.getDescription())
                                .orderIndex(stepReq.getOrderIndex())
                                .optional(stepReq.isOptional())
                                .build());
            }
        }

        // 3️⃣ DELETE removed steps
        for (WorkflowStep step : existingSteps) {
            if (!incomingIds.contains(step.getId())) {
                stepRepository.delete(step);
            }
        }

        return map(workflow);
    }

    @Override
    public WorkflowWithStepsResponse getWorkflowWithSteps(Long workflowId, Long companyId) {

        Workflow workflow = workflowRepository.findById(workflowId)
                .filter(w -> w.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow not found"));

        List<WorkflowStepResponse> steps = stepRepository
                .findByWorkflowIdOrderByOrderIndexAsc(workflowId)
                .stream()
                .map(this::map)
                .toList();

        return WorkflowWithStepsResponse.builder()
                .id(workflow.getId())
                .companyId(workflow.getCompany().getId())
                .name(workflow.getName())
                .description(workflow.getDescription())
                .steps(steps)
                .build();
    }

    private WorkflowResponse map(Workflow w) {
        return WorkflowResponse.builder()
                .id(w.getId())
                .companyId(w.getCompany().getId())
                .name(w.getName())
                .description(w.getDescription())
                .build();
    }

    private WorkflowStepResponse map(WorkflowStep s) {
        return WorkflowStepResponse.builder()
                .id(s.getId())
                .workflowId(s.getWorkflow().getId())
                .name(s.getName())
                .description(s.getDescription())
                .orderIndex(s.getOrderIndex())
                .optional(s.isOptional())
                .build();
    }

}
