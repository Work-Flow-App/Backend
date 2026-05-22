package com.workflow.controller.workflow;

import com.workflow.common.security.RequireCompanyRole;
import com.workflow.common.util.AuthUtils;
import com.workflow.dto.workflow.*;
import com.workflow.service.workflow.IWorkflowService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

import static com.workflow.common.constant.CompanyRole.*;

@Tag(name = "Workflows")
@RestController
@RequestMapping("/api/v1/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final IWorkflowService workflowService;

    private Long companyId() {
        return AuthUtils.getCompanyId();
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR})
    @PostMapping
    public ResponseEntity<WorkflowResponse> create(
            @Valid @RequestBody WorkflowCreateRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workflowService.createWorkflow(request, companyId()));
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR})
    @PutMapping("/{id}")
    public WorkflowResponse update(
            @PathVariable Long id,
            @Valid @RequestBody WorkflowCreateRequest request,
            Authentication auth) {
        return workflowService.updateWorkflow(id, request, companyId());
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER})
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id,
            Authentication auth) {
        workflowService.deleteWorkflow(id, companyId());
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER})
    @PatchMapping("/{id}/archive")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(
            @PathVariable Long id,
            Authentication auth) {
        workflowService.archiveWorkflow(id, companyId());
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR, VIEWER})
    @GetMapping
    public List<WorkflowResponse> getAll(Authentication auth) {
        return workflowService.getAllWorkflows(companyId());
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR, VIEWER})
    @GetMapping("/{id}")
    public WorkflowResponse getOne(
            @PathVariable Long id,
            Authentication auth) {
        return workflowService.getWorkflow(id, companyId());
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR})
    @PostMapping("/steps")
    public WorkflowStepResponse createStep(
            @Valid @RequestBody WorkflowStepCreateRequest request,
            Authentication auth) {
        return workflowService.createStep(request, companyId());
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR, VIEWER})
    @GetMapping("/{workflowId}/steps")
    public List<WorkflowStepResponse> getSteps(
            @PathVariable Long workflowId,
            Authentication auth) {
        return workflowService.getSteps(workflowId, companyId());
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR, VIEWER})
    @GetMapping("/steps/{stepId}")
    public WorkflowStepResponse getStep(
            @PathVariable Long stepId,
            Authentication auth) {
        return workflowService.getStep(stepId, companyId());
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR, VIEWER})
    @GetMapping("/steps")
    public List<WorkflowStepResponse> getAllSteps(Authentication auth) {
        return workflowService.getAllSteps(companyId());
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR})
    @PutMapping("/steps/{stepId}")
    public WorkflowStepResponse updateStep(
            @PathVariable Long stepId,
            @Valid @RequestBody WorkflowStepCreateRequest request,
            Authentication auth) {
        return workflowService.updateStep(stepId, request, companyId());
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER})
    @DeleteMapping("/steps/{stepId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStep(@PathVariable Long stepId, Authentication auth) {
        workflowService.deleteStep(stepId, companyId());
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR})
    @PutMapping("/{workflowId}/bulk")
    public ResponseEntity<WorkflowResponse> bulkUpdate(
            @PathVariable Long workflowId,
            @Valid @RequestBody WorkflowBulkUpdateRequest request,
            Authentication auth) {
        return ResponseEntity.ok(workflowService.bulkUpdateWorkflow(workflowId, request, companyId()));
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR, VIEWER})
    @GetMapping("/{workflowId}/with-steps")
    public WorkflowWithStepsResponse getWorkflowWithSteps(
            @PathVariable Long workflowId,
            Authentication auth) {
        return workflowService.getWorkflowWithSteps(workflowId, companyId());
    }
}
