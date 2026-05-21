package com.workflow.controller.workflow;

import com.workflow.common.security.RequireCompanyRole;
import com.workflow.common.util.AuthUtils;
import com.workflow.dto.workflow.StepVisitLogCreateRequest;
import com.workflow.dto.workflow.StepVisitLogResponse;
import com.workflow.dto.workflow.StepVisitLogSummaryResponse;
import com.workflow.service.workflow.IJobWorkflowStepVisitLogService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import static com.workflow.common.constant.CompanyRole.*;

@Tag(name = "Workflow Step Visit Logs")
@RestController
@RequestMapping("/api/v1/job-workflow-steps")
@RequiredArgsConstructor
public class JobWorkflowStepVisitLogController {

    private final IJobWorkflowStepVisitLogService visitLogService;

    private Long getCompanyId() {
        return AuthUtils.getCompanyId();
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR})
    @PostMapping("/{stepId}/visits")
    public ResponseEntity<StepVisitLogResponse> addVisitLog(
            @PathVariable Long stepId,
            @Valid @RequestBody StepVisitLogCreateRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(visitLogService.addVisitLog(stepId, request, getCompanyId()));
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR})
    @PutMapping("/visits/{visitLogId}")
    public ResponseEntity<StepVisitLogResponse> updateVisitLog(
            @PathVariable Long visitLogId,
            @Valid @RequestBody StepVisitLogCreateRequest request,
            Authentication auth) {
        return ResponseEntity.ok(visitLogService.updateVisitLog(visitLogId, request, getCompanyId()));
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER})
    @DeleteMapping("/visits/{visitLogId}")
    public ResponseEntity<Void> deleteVisitLog(
            @PathVariable Long visitLogId,
            Authentication auth) {
        visitLogService.deleteVisitLog(visitLogId, getCompanyId());
        return ResponseEntity.noContent().build();
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR, VIEWER})
    @GetMapping("/{stepId}/visits")
    public ResponseEntity<StepVisitLogSummaryResponse> getVisitLogs(
            @PathVariable Long stepId,
            Authentication auth) {
        return ResponseEntity.ok(visitLogService.getVisitLogs(stepId, getCompanyId()));
    }
}
