package com.workflow.controller.company;

import com.workflow.dto.company.CompanyDashboardResponse;
import com.workflow.dto.company.CompanyProfileResponse;
import com.workflow.dto.company.CompanyProfileUpdateRequest;
import com.workflow.entity.User;
import com.workflow.service.company.ICompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Company")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/companies")
public class CompanyController {

    private final ICompanyService companyService;

    @PostMapping("/profile")
    public ResponseEntity<CompanyProfileResponse> updateProfile(
            @Valid @RequestBody CompanyProfileUpdateRequest request,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        CompanyProfileResponse response = companyService.updateProfile(request, user.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/profile")
    public ResponseEntity<CompanyProfileResponse> getProfile(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        CompanyProfileResponse response = companyService.getProfile(user.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/dashboard")
    public ResponseEntity<CompanyDashboardResponse> getDashboard(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        CompanyDashboardResponse response = companyService.getDashboard(user.getId());
        return ResponseEntity.ok(response);
    }
}