package com.workflow.controller.company;

import com.workflow.common.security.RequireCompanyRole;
import com.workflow.dto.company.CompanyDashboardResponse;
import com.workflow.dto.company.CompanyProfileResponse;
import com.workflow.dto.company.CompanyProfileUpdateRequest;
import com.workflow.entity.auth.User;
import com.workflow.service.company.ICompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;

import static com.workflow.common.constant.CompanyRole.*;

@Tag(name = "Company")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/companies")
public class CompanyController {

    private final ICompanyService companyService;

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER})
    @PostMapping("/profile")
    public ResponseEntity<CompanyProfileResponse> updateProfile(
            @Valid @RequestBody CompanyProfileUpdateRequest request,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(companyService.updateProfile(request, user.getId()));
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR, VIEWER})
    @GetMapping("/profile")
    public ResponseEntity<CompanyProfileResponse> getProfile(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(companyService.getProfile(user.getId()));
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR, VIEWER})
    @GetMapping("/dashboard")
    public ResponseEntity<CompanyDashboardResponse> getDashboard(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(companyService.getDashboard(user.getId()));
    }
}
