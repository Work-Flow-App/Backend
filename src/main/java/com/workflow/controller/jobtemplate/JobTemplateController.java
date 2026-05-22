package com.workflow.controller.jobtemplate;

import com.workflow.common.security.RequireCompanyRole;
import com.workflow.common.util.AuthUtils;
import com.workflow.dto.jobtemplate.*;
import com.workflow.service.jobtemplate.IJobTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

import static com.workflow.common.constant.CompanyRole.*;

@Tag(name = "Job Templates")
@RestController
@RequestMapping("/api/v1/job-templates")
@RequiredArgsConstructor
public class JobTemplateController {

    private final IJobTemplateService service;

    private Long getCompanyId() {
        return AuthUtils.getCompanyId();
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR})
    @PostMapping
    public ResponseEntity<JobTemplateResponse> createTemplate(
            @Valid @RequestBody JobTemplateCreateRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createTemplate(request, getCompanyId()));
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR, VIEWER})
    @GetMapping
    public ResponseEntity<List<JobTemplateResponse>> getAllTemplates(Authentication auth) {
        return ResponseEntity.ok(service.getAllTemplates(getCompanyId()));
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR, VIEWER})
    @GetMapping("/{id}")
    public ResponseEntity<JobTemplateResponse> getTemplate(
            @PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(service.getTemplate(id, getCompanyId()));
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR, VIEWER})
    @GetMapping("/default")
    public ResponseEntity<JobTemplateResponse> getDefaultTemplate(Authentication auth) {
        return service.getDefaultTemplate(getCompanyId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR})
    @PutMapping("/{id}")
    public ResponseEntity<JobTemplateResponse> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody JobTemplateCreateRequest request,
            Authentication auth) {
        return ResponseEntity.ok(service.updateTemplate(id, request, getCompanyId()));
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id, Authentication auth) {
        service.deleteTemplate(id, getCompanyId());
        return ResponseEntity.noContent().build();
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR})
    @PostMapping("/fields")
    public ResponseEntity<JobTemplateFieldResponse> createTemplateField(
            @Valid @RequestBody JobTemplateFieldCreateRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createTemplateField(request, getCompanyId()));
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR, VIEWER})
    @GetMapping("/{id}/fields")
    public ResponseEntity<List<JobTemplateFieldResponse>> getTemplateFields(
            @PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(service.getFieldsByTemplate(id, getCompanyId()));
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR, VIEWER})
    @GetMapping("/fields/{fieldId}")
    public ResponseEntity<JobTemplateFieldResponse> getTemplateField(
            @PathVariable Long fieldId, Authentication auth) {
        return ResponseEntity.ok(service.getField(fieldId, getCompanyId()));
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR})
    @PutMapping("/fields/{fieldId}")
    public ResponseEntity<JobTemplateFieldResponse> updateTemplateField(
            @PathVariable Long fieldId,
            @Valid @RequestBody JobTemplateFieldCreateRequest request,
            Authentication auth) {
        return ResponseEntity.ok(service.updateField(fieldId, request, getCompanyId()));
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER})
    @DeleteMapping("/fields/{fieldId}")
    public ResponseEntity<Void> deleteTemplateField(
            @PathVariable Long fieldId, Authentication auth) {
        service.deleteField(fieldId, getCompanyId());
        return ResponseEntity.noContent().build();
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR, VIEWER})
    @GetMapping("/{id}/with-fields")
    public ResponseEntity<JobTemplateWithFieldsResponse> getTemplateWithFields(
            @PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(service.getTemplateWithFields(id, getCompanyId()));
    }
}
