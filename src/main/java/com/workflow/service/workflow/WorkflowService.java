package com.workflow.service.workflow;

import com.workflow.dto.workflow.*;
import com.workflow.entity.*;
import com.workflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkflowService implements IWorkflowService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowStepRepository stepRepository;
    private final CompanyRepository companyRepository;

    @Override
    public WorkflowResponse createWorkflow(WorkflowCreateRequest request, Long companyId) {
        Company company = companyRepository.findById(companyId).orElseThrow();

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
                .orElseThrow();

        workflow.setName(request.getName());
        workflow.setDescription(request.getDescription());

        return map(workflow);
    }

    @Override
    public void deleteWorkflow(Long id, Long companyId) {
        Workflow workflow = workflowRepository.findById(id)
                .filter(w -> w.getCompany().getId().equals(companyId))
                .orElseThrow();

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
                .orElseThrow(() -> new IllegalStateException("Workflow not found"));

        return map(workflow);
    }

    @Override
    public WorkflowStepResponse createStep(WorkflowStepCreateRequest request, Long companyId) {
        Workflow workflow = workflowRepository.findById(request.getWorkflowId())
                .filter(w -> w.getCompany().getId().equals(companyId))
                .orElseThrow();

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
        return stepRepository.findByWorkflowIdOrderByOrderIndexAsc(workflowId)
                .stream().map(this::map).collect(Collectors.toList());
    }

    @Transactional
    public WorkflowStepResponse updateStep(Long stepId, WorkflowStepCreateRequest request, Long companyId) {
        WorkflowStep step = stepRepository.findById(stepId)
                .filter(s -> s.getWorkflow().getCompany().getId().equals(companyId))
                .orElseThrow(() -> new IllegalStateException("Step not found"));

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
                .orElseThrow(() -> new IllegalStateException("Workflow step not found"));

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

    @Transactional
    public void deleteStep(Long stepId, Long companyId) {
        WorkflowStep step = stepRepository.findById(stepId)
                .filter(s -> s.getWorkflow().getCompany().getId().equals(companyId))
                .orElseThrow(() -> new IllegalStateException("Step not found"));
        stepRepository.delete(step);
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
