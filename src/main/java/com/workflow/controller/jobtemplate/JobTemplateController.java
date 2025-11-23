package com.workflow.controller.jobtemplate;

import com.workflow.dto.jobtemplate.*;
import com.workflow.entity.Company;
import com.workflow.entity.User;
import com.workflow.repository.CompanyRepository;
import com.workflow.service.company.ICompanyService;
import com.workflow.service.jobtemplate.IJobTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/job-templates")
@RequiredArgsConstructor
public class JobTemplateController {

    private final IJobTemplateService templateService;
    private final ICompanyService companyService;

    private Long getCompanyId(Authentication auth) {
        User user = (User) auth.getPrincipal();
        Company company = companyService.findCompanyByUserId(user.getId());
        return company.getId();
    }

    @PostMapping
    public ResponseEntity<JobTemplateResponse> create(
            @RequestBody JobTemplateCreateRequest request,
            Authentication auth
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(templateService.createTemplate(request, getCompanyId(auth)));
    }

    @GetMapping
    public ResponseEntity<List<JobTemplateResponse>> getAll(Authentication auth) {
        return ResponseEntity.ok(templateService.getAllTemplates(getCompanyId(auth)));
    }

    @PostMapping("/fields")
    public ResponseEntity<JobTemplateFieldResponse> createField(
            @RequestBody JobTemplateFieldCreateRequest request,
            Authentication auth
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(templateService.createTemplateField(request, getCompanyId(auth)));
    }
}
