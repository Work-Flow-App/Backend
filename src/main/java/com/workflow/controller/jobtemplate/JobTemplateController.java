package com.workflow.controller.jobtemplate;

import com.workflow.dto.jobtemplate.*;
import com.workflow.entity.Company;
import com.workflow.entity.User;
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

    private final IJobTemplateService service;
    private final ICompanyService companyService;

    private Long getCompanyId(Authentication auth) {
        User user = (User) auth.getPrincipal();
        Company company = companyService.findCompanyByUserId(user.getId());
        return company.getId();
    }

    // -------- TEMPLATE CRUD --------

    @PostMapping
    public ResponseEntity<JobTemplateResponse> createTemplate(
            @RequestBody JobTemplateCreateRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createTemplate(request, getCompanyId(auth)));
    }

    @GetMapping
    public ResponseEntity<List<JobTemplateResponse>> getAllTemplates(Authentication auth) {
        return ResponseEntity.ok(service.getAllTemplates(getCompanyId(auth)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobTemplateResponse> getTemplate(
            @PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(service.getTemplate(id, getCompanyId(auth)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<JobTemplateResponse> updateTemplate(
            @PathVariable Long id,
            @RequestBody JobTemplateCreateRequest request,
            Authentication auth) {
        return ResponseEntity.ok(service.updateTemplate(id, request, getCompanyId(auth)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id, Authentication auth) {
        service.deleteTemplate(id, getCompanyId(auth));
        return ResponseEntity.noContent().build();
    }

    // -------- TEMPLATE FIELDS CRUD --------

    @PostMapping("/fields")
    public ResponseEntity<JobTemplateFieldResponse> createTemplateField(
            @RequestBody JobTemplateFieldCreateRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createTemplateField(request, getCompanyId(auth)));
    }

    @GetMapping("/{id}/fields")
    public ResponseEntity<List<JobTemplateFieldResponse>> getTemplateFields(
            @PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(service.getFieldsByTemplate(id, getCompanyId(auth)));
    }

    @GetMapping("/fields/{fieldId}")
    public ResponseEntity<JobTemplateFieldResponse> getTemplateField(
            @PathVariable Long fieldId, Authentication auth) {
        return ResponseEntity.ok(service.getField(fieldId, getCompanyId(auth)));
    }

    @PutMapping("/fields/{fieldId}")
    public ResponseEntity<JobTemplateFieldResponse> updateTemplateField(
            @PathVariable Long fieldId,
            @RequestBody JobTemplateFieldCreateRequest request,
            Authentication auth) {
        return ResponseEntity.ok(service.updateField(fieldId, request, getCompanyId(auth)));
    }

    @DeleteMapping("/fields/{fieldId}")
    public ResponseEntity<Void> deleteTemplateField(
            @PathVariable Long fieldId, Authentication auth) {
        service.deleteField(fieldId, getCompanyId(auth));
        return ResponseEntity.noContent().build();
    }

    // -------- TEMPLATE + FIELDS --------
    @GetMapping("/{id}/with-fields")
    public ResponseEntity<JobTemplateWithFieldsResponse> getTemplateWithFields(
            @PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(service.getTemplateWithFields(id, getCompanyId(auth)));
    }
}
