package com.workflow.controller.workflow;

import com.workflow.dto.workflow.StepVisitLogCreateRequest;
import com.workflow.dto.workflow.StepVisitLogResponse;
import com.workflow.entity.Company;
import com.workflow.entity.User;
import com.workflow.service.company.ICompanyService;
import com.workflow.service.workflow.IJobWorkflowStepVisitLogService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Workflow Step Visit Logs")
@RestController
@RequestMapping("/api/v1/job-workflow-steps")
@RequiredArgsConstructor
public class JobWorkflowStepVisitLogController {

    private final IJobWorkflowStepVisitLogService visitLogService;
    private final ICompanyService companyService;

    private Long getCompanyId(Authentication auth) {
        User user = (User) auth.getPrincipal();
        Company company = companyService.findCompanyByUserId(user.getId());
        return company.getId();
    }

    @PostMapping("/{stepId}/visits")
    public ResponseEntity<StepVisitLogResponse> addVisitLog(
            @PathVariable Long stepId,
            @RequestBody StepVisitLogCreateRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                visitLogService.addVisitLog(stepId, request, getCompanyId(auth)));
    }

    @PutMapping("/visits/{visitLogId}")
    public ResponseEntity<StepVisitLogResponse> updateVisitLog(
            @PathVariable Long visitLogId,
            @RequestBody StepVisitLogCreateRequest request,
            Authentication auth) {
        return ResponseEntity.ok(
                visitLogService.updateVisitLog(visitLogId, request, getCompanyId(auth)));
    }

    @DeleteMapping("/visits/{visitLogId}")
    public ResponseEntity<Void> deleteVisitLog(
            @PathVariable Long visitLogId,
            Authentication auth) {
        visitLogService.deleteVisitLog(visitLogId, getCompanyId(auth));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{stepId}/visits")
    public ResponseEntity<List<StepVisitLogResponse>> getVisitLogs(
            @PathVariable Long stepId,
            Authentication auth) {
        return ResponseEntity.ok(
                visitLogService.getVisitLogs(stepId, getCompanyId(auth)));
    }
}