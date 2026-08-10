package com.workflow.controller.form;

import com.workflow.dto.form.FormAttachmentResponse;
import com.workflow.dto.form.FormFieldValueDto;
import com.workflow.dto.form.FormSubmissionResponse;
import com.workflow.entity.auth.User;
import com.workflow.repository.worker.WorkerRepository;
import com.workflow.service.form.IFormService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.util.List;

@Tag(name = "Worker Forms")
@RestController
@RequestMapping("/api/v1/worker/forms")
@RequiredArgsConstructor
public class WorkerFormController {

    private final IFormService formService;
    private final WorkerRepository workerRepository;

    private Long getWorkerId(Authentication auth) {
        User user = (User) auth.getPrincipal();

        // Using your exact method to bridge the Auth User to the Worker Profile
        return workerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Worker profile not found for user ID: " + user.getId()))
                .getId();
    }

    @GetMapping
    public ResponseEntity<List<FormSubmissionResponse>> getMyForms(Authentication auth) {
        return ResponseEntity.ok(formService.getAssignedForms(getWorkerId(auth)));
    }

    @PutMapping("/{id}/values")
    public ResponseEntity<FormSubmissionResponse> fillValues(@PathVariable Long id,
            @RequestBody List<FormFieldValueDto> values, Authentication auth) {
        formService.markInProgress(id, getWorkerId(auth));
        return ResponseEntity.ok(formService.updateValues(id, values, getWorkerId(auth), true));
    }

    @PostMapping(value = "/{id}/fields/{fieldId}/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FormSubmissionResponse> uploadFile(
            @PathVariable Long id, @PathVariable Long fieldId, @RequestParam("file") MultipartFile file,
            Authentication auth) throws IOException {
        formService.markInProgress(id, getWorkerId(auth));
        return ResponseEntity.ok(formService.uploadFile(id, fieldId, file, getWorkerId(auth), true));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<FormSubmissionResponse> submitForm(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(formService.submitForm(id, getWorkerId(auth)));
    }

    @GetMapping("/{id}/attachments")
    public ResponseEntity<List<FormAttachmentResponse>> getAttachments(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(formService.getFormAttachments(id, getWorkerId(auth), true));
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id, Authentication auth) throws Exception {

        byte[] pdfBytes = formService.generateFormPdf(id, getWorkerId(auth), true);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"form_submission_" + id + ".pdf\"")
                .body(pdfBytes);
    }
}