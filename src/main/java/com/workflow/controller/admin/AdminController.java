package com.workflow.controller.admin;

import com.workflow.dto.admin.AdminCompanyResponse;
import com.workflow.dto.admin.AdminJobResponse;
import com.workflow.dto.admin.AdminWorkerResponse;
import com.workflow.service.admin.IAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final IAdminService adminService;

    @GetMapping("/companies")
    public ResponseEntity<Page<AdminCompanyResponse>> getAllCompanies(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(adminService.getAllCompanies(pageable));
    }

    @GetMapping("/workers")
    public ResponseEntity<Page<AdminWorkerResponse>> getAllWorkers(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(adminService.getAllWorkers(pageable));
    }

    @GetMapping("/jobs")
    public ResponseEntity<Page<AdminJobResponse>> getAllJobs(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(adminService.getAllJobs(pageable));
    }
}
