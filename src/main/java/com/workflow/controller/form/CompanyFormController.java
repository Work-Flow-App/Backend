package com.workflow.controller.form;

import com.workflow.common.security.RequireCompanyRole;
import com.workflow.common.util.AuthUtils;
import com.workflow.dto.form.*;
import com.workflow.service.form.IFormService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.util.List;
import static com.workflow.common.constant.CompanyRole.*;

@Tag(name = "Company Forms")
@RestController
@RequestMapping("/api/v1/forms")
@RequiredArgsConstructor
public class CompanyFormController {

    private final IFormService formService;

    @RequireCompanyRole({ COMPANY_ADMIN, MANAGER })
    @PostMapping("/templates")
    public ResponseEntity<FormTemplateRequest> createTemplate(@RequestBody FormTemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(formService.createTemplate(request, AuthUtils.getCompanyId()));
    }

    @RequireCompanyRole({ COMPANY_ADMIN, MANAGER, EDITOR })
    @PostMapping("/drafts")
    public ResponseEntity<FormSubmissionResponse> createDraft(@RequestBody FormSubmissionCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(formService.createDraft(request, AuthUtils.getCompanyId()));
    }

    @RequireCompanyRole({ COMPANY_ADMIN, MANAGER, EDITOR })
    @PutMapping("/{id}/values")
    public ResponseEntity<FormSubmissionResponse> updateValues(@PathVariable Long id,
            @RequestBody List<FormFieldValueDto> values) {
        return ResponseEntity.ok(formService.updateValues(id, values, AuthUtils.getCompanyId(), false));
    }

    @RequireCompanyRole({ COMPANY_ADMIN, MANAGER, EDITOR })
    @PostMapping(value = "/{id}/fields/{fieldId}/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FormSubmissionResponse> uploadFile(
            @PathVariable Long id, @PathVariable Long fieldId, @RequestParam("file") MultipartFile file)
            throws IOException {
        return ResponseEntity.ok(formService.uploadFile(id, fieldId, file, AuthUtils.getCompanyId(), false));
    }

    @RequireCompanyRole({ COMPANY_ADMIN, MANAGER })
    @PostMapping("/{id}/send/{workerId}")
    public ResponseEntity<FormSubmissionResponse> sendToWorker(@PathVariable Long id, @PathVariable Long workerId) {
        return ResponseEntity.ok(formService.sendToWorker(id, workerId, AuthUtils.getCompanyId()));
    }

    @RequireCompanyRole({ COMPANY_ADMIN, MANAGER })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubmission(@PathVariable Long id) {
        formService.deleteSubmission(id, AuthUtils.getCompanyId());
        return ResponseEntity.noContent().build();
    }

    @RequireCompanyRole({ COMPANY_ADMIN, MANAGER, EDITOR, VIEWER })
    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) throws Exception {

        byte[] pdfBytes = formService.generateFormPdf(id, AuthUtils.getCompanyId(), false);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"form_submission_" + id + ".pdf\"")
                .body(pdfBytes);
    }

    @RequireCompanyRole({ COMPANY_ADMIN, MANAGER, EDITOR, VIEWER })
    @GetMapping("/{id}/attachments")
    public ResponseEntity<List<FormAttachmentResponse>> getAttachments(@PathVariable Long id) {
        return ResponseEntity.ok(formService.getFormAttachments(id, AuthUtils.getCompanyId(), false));
    }

    @RequireCompanyRole({ COMPANY_ADMIN, MANAGER, EDITOR, VIEWER })
    @GetMapping("/templates")
    public ResponseEntity<List<FormTemplateRequest>> getAllTemplates() {
        return ResponseEntity.ok(formService.getTemplates(AuthUtils.getCompanyId()));
    }

    // List all submissions
    @RequireCompanyRole({ COMPANY_ADMIN, MANAGER, EDITOR, VIEWER })
    @GetMapping
    public ResponseEntity<List<FormSubmissionResponse>> getAllSubmissions() {
        // You'll need to add a method to IFormService to list by company
        return ResponseEntity.ok(formService.getCompanySubmissions(AuthUtils.getCompanyId()));
    }

    @RequireCompanyRole({ COMPANY_ADMIN, MANAGER, EDITOR, VIEWER })
    @DeleteMapping("/templates/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id) {
        formService.deleteTemplate(id, AuthUtils.getCompanyId());
        return ResponseEntity.noContent().build();
    }

    // Add this endpoint back
    @RequireCompanyRole({ COMPANY_ADMIN, MANAGER, EDITOR, VIEWER })
    @PutMapping("/templates/{id}")
    public ResponseEntity<FormTemplateRequest> updateTemplate(@PathVariable Long id,
            @RequestBody FormTemplateRequest request) {
        return ResponseEntity.ok(formService.updateTemplate(id, request, AuthUtils.getCompanyId()));
    }
}
