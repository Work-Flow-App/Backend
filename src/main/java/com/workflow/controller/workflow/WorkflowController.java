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

    private final IWorkflowService service;
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
                .body(service.createWorkflow(request, companyId(auth)));
    }

    @PutMapping("/{id}")
    public WorkflowResponse update(
            @PathVariable Long id,
            @RequestBody WorkflowCreateRequest request,
            Authentication auth) {
        return service.updateWorkflow(id, request, companyId(auth));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id,
            Authentication auth) {
        service.deleteWorkflow(id, companyId(auth));
    }

    @GetMapping
    public List<WorkflowResponse> getAll(Authentication auth) {
        return service.getAllWorkflows(companyId(auth));
    }

    @GetMapping("/{id}")
    public WorkflowResponse getOne(
            @PathVariable Long id,
            Authentication auth) {
        return service.getWorkflow(id, companyId(auth));
    }

    @PostMapping("/steps")
    public WorkflowStepResponse createStep(
            @RequestBody WorkflowStepCreateRequest request,
            Authentication auth) {
        return service.createStep(request, companyId(auth));
    }

    @GetMapping("/{workflowId}/steps")
    public List<WorkflowStepResponse> getSteps(
            @PathVariable Long workflowId,
            Authentication auth) {
        return service.getSteps(workflowId, companyId(auth));
    }
}