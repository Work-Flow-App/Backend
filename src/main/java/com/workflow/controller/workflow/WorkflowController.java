package com.workflow.controller.workflow;

import com.workflow.dto.workflow.*;
import com.workflow.entity.Company;
import com.workflow.entity.User;
import com.workflow.service.company.ICompanyService;
import com.workflow.service.workflow.IWorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final IWorkflowService workflowService;
    private final ICompanyService companyService;

    private Long companyId(Authentication auth) {
        User user = (User) auth.getPrincipal();
        Company company = companyService.findCompanyByUserId(user.getId());
        return company.getId();
    }

    @PostMapping
    public ResponseEntity<WorkflowResponse> create(
            @RequestBody WorkflowCreateRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workflowService.createWorkflow(request, companyId(auth)));
    }

    @PutMapping("/{id}")
    public WorkflowResponse update(
            @PathVariable Long id,
            @RequestBody WorkflowCreateRequest request,
            Authentication auth) {
        return workflowService.updateWorkflow(id, request, companyId(auth));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id,
            Authentication auth) {
        workflowService.deleteWorkflow(id, companyId(auth));
    }

    @GetMapping
    public List<WorkflowResponse> getAll(Authentication auth) {
        return workflowService.getAllWorkflows(companyId(auth));
    }

    @GetMapping("/{id}")
    public WorkflowResponse getOne(
            @PathVariable Long id,
            Authentication auth) {
        return workflowService.getWorkflow(id, companyId(auth));
    }

    @PostMapping("/steps")
    public WorkflowStepResponse createStep(
            @RequestBody WorkflowStepCreateRequest request,
            Authentication auth) {
        return workflowService.createStep(request, companyId(auth));
    }

    @GetMapping("/{workflowId}/steps")
    public List<WorkflowStepResponse> getSteps(
            @PathVariable Long workflowId,
            Authentication auth) {
        return workflowService.getSteps(workflowId, companyId(auth));
    }

    @GetMapping("/steps/{stepId}")
    public WorkflowStepResponse getStep(
            @PathVariable Long stepId,
            Authentication auth) {
        return workflowService.getStep(stepId, companyId(auth));
    }

    @GetMapping("/steps")
    public List<WorkflowStepResponse> getAllSteps(Authentication auth) {
        return workflowService.getAllSteps(companyId(auth));
    }

    @PutMapping("/steps/{stepId}")
    public WorkflowStepResponse updateStep(
            @PathVariable Long stepId,
            @RequestBody WorkflowStepCreateRequest request,
            Authentication auth) {
        return workflowService.updateStep(stepId, request, companyId(auth));
    }

    @DeleteMapping("/steps/{stepId}")
    public void deleteStep(@PathVariable Long stepId, Authentication auth) {
        workflowService.deleteStep(stepId, companyId(auth));
    }

    @PutMapping("/{workflowId}/bulk")
    public ResponseEntity<WorkflowResponse> bulkUpdate(
            @PathVariable Long workflowId,
            @RequestBody WorkflowBulkUpdateRequest request,
            Authentication auth) {

        Long companyId = companyId(auth);
        return ResponseEntity.ok(
                workflowService.bulkUpdateWorkflow(workflowId, request, companyId));
    }

    @GetMapping("/{workflowId}/with-steps")
    public WorkflowWithStepsResponse getWorkflowWithSteps(
            @PathVariable Long workflowId,
            Authentication auth) {

        return workflowService.getWorkflowWithSteps(workflowId, companyId(auth));
    }

}