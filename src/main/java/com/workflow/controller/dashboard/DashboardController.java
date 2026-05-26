package com.workflow.controller.dashboard;

import com.workflow.common.security.RequireCompanyRole;
import com.workflow.common.util.AuthUtils;
import com.workflow.dto.dashboard.FinancialSummaryResponse;
import com.workflow.service.dashboard.IDashboardService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.workflow.common.constant.CompanyRole.*;

@Tag(name = "Dashboard")
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final IDashboardService dashboardService;

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR, VIEWER})
    @GetMapping("/financial-summary")
    public ResponseEntity<FinancialSummaryResponse> getFinancialSummary(Authentication auth) {
        return ResponseEntity.ok(dashboardService.getFinancialSummary(AuthUtils.getCompanyId()));
    }
}
