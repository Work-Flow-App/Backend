package com.workflow.service.form;

import com.workflow.common.constant.form.FormFieldType;
import com.workflow.common.constant.form.FormSubmissionStatus;
import com.workflow.common.exception.business.*;
import com.workflow.dto.form.*;
import com.workflow.entity.company.Company;
import com.workflow.entity.form.*;
import com.workflow.entity.worker.Worker;
import com.workflow.repository.company.CompanyRepository;
import com.workflow.repository.form.*;
import com.workflow.repository.worker.WorkerRepository;
import com.workflow.service.storage.IStorageService;
import com.workflow.templates.pdf.form.FormPdfRenderer;

import lombok.RequiredArgsConstructor;

import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FormService implements IFormService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    private final FormTemplateRepository templateRepo;
    private final FormFieldRepository fieldRepo;
    private final FormSubmissionRepository submissionRepo;
    private final CompanyRepository companyRepo;
    private final WorkerRepository workerRepo;
    private final IStorageService s3Service;
    private final FormPdfRenderer formPdfRenderer;
    private final Tika tika;
    @Value("${workflow.security.file.blocked-types}")
    private List<String> blockedTypes;

    @Override
    public FormTemplateRequest createTemplate(FormTemplateRequest request, Long companyId) {
        Company company = companyRepo.findById(companyId).orElseThrow();

        FormTemplate template = FormTemplate.builder()
                .company(company)
                .name(request.getName())
                .description(request.getDescription())
                .version(1) // explicitly set version 1
                .build();

        List<FormField> fields = request.getFields().stream().map(dto -> FormField.builder()
                .template(template)
                .name(dto.getName())
                .label(dto.getLabel())
                .type(dto.getType())
                .roleTarget(dto.getRoleTarget())
                .required(dto.isRequired())
                .options(dto.getOptions())
                .orderIndex(dto.getOrderIndex())
                .build()).collect(Collectors.toList());

        template.setFields(fields);
        templateRepo.save(template);
        return request;
    }

    @Override
    public List<FormTemplateRequest> getTemplates(Long companyId) {
        return templateRepo.findByCompanyId(companyId).stream().map(t -> FormTemplateRequest.builder()
                .id(t.getId())
                .name(t.getName())
                .description(t.getDescription())
                .version(t.getVersion()) // expose version
                .parentTemplateId(t.getParentTemplateId())
                .archived(t.isArchived())
                .fields(t.getFields().stream().map(f -> FormFieldDto.builder()
                        .id(f.getId())
                        .name(f.getName())
                        .label(f.getLabel())
                        .type(f.getType())
                        .roleTarget(f.getRoleTarget())
                        .required(f.isRequired())
                        .options(f.getOptions())
                        .orderIndex(f.getOrderIndex())
                        .build()).collect(Collectors.toList()))
                .build()).collect(Collectors.toList());
    }

    @Override
    public FormTemplateRequest updateTemplate(Long templateId, FormTemplateRequest request, Long companyId) {
        // 1. Fetch the old active template
        FormTemplate oldTemplate = templateRepo.findByIdAndCompanyIdAndArchivedFalse(templateId, companyId)
                .orElseThrow(() -> new TemplateNotFoundException("Template not found or already archived"));

        // 2. Secretly Archive the old template so historical forms remain intact
        oldTemplate.setArchived(true);
        templateRepo.save(oldTemplate);

        // 3. Figure out the "Parent" (original V1 template) for grouping later if
        // needed
        Long parentId = oldTemplate.getParentTemplateId() != null ? oldTemplate.getParentTemplateId()
                : oldTemplate.getId();

        // 4. Create the brand NEW template version
        FormTemplate newTemplate = FormTemplate.builder()
                .company(oldTemplate.getCompany())
                .name(request.getName()) // Can be updated name
                .description(request.getDescription()) // Can be updated description
                .version(oldTemplate.getVersion() + 1) // Increment Version!
                .parentTemplateId(parentId)
                .archived(false)
                .build();

        // 5. Clone fields from request directly to the NEW template.
        // We ignore the incoming DTO field IDs because these are entirely new fields in
        // the DB!
        List<FormField> newFields = request.getFields().stream().map(dto -> FormField.builder()
                .template(newTemplate)
                .name(dto.getName())
                .label(dto.getLabel())
                .type(dto.getType())
                .roleTarget(dto.getRoleTarget())
                .required(dto.isRequired())
                .options(dto.getOptions())
                .orderIndex(dto.getOrderIndex())
                .build()).collect(Collectors.toList());

        newTemplate.setFields(newFields);

        // 6. Save and return
        FormTemplate savedTemplate = templateRepo.save(newTemplate);

        request.setId(savedTemplate.getId());
        request.setVersion(savedTemplate.getVersion());
        request.setParentTemplateId(savedTemplate.getParentTemplateId());

        return request;
    }

    @Override
    public void deleteTemplate(Long templateId, Long companyId) {
        // CHANGED: Fetch regardless of whether it's archived or not
        FormTemplate template = templateRepo.findByIdAndCompanyId(templateId, companyId)
                .orElseThrow(() -> new TemplateNotFoundException("Template not found"));

        long submissionCount = submissionRepo.countByTemplateId(templateId);

        if (submissionCount == 0) {
            // STEP 2: No submissions left! We can safely hard delete it forever.
            templateRepo.delete(template);
        } else {
            // Forms exist!
            if (template.isArchived()) {
                // If it's ALREADY archived, but they clicked delete again, we must stop them.
                throw new InvalidRequestException(
                        "Cannot permanently delete. Please delete the " + submissionCount + " associated forms first.");
            } else {
                // STEP 1: It's active. Soft delete (Archive) it.
                template.setArchived(true);
                templateRepo.save(template);
            }
        }
    }

    @Override
    public FormSubmissionResponse createDraft(FormSubmissionCreateRequest request, Long companyId) {
        Company company = companyRepo.findById(companyId).orElseThrow();
        FormTemplate template = templateRepo.findByIdAndCompanyIdAndArchivedFalse(request.getTemplateId(), companyId)
                .orElseThrow(() -> new TemplateNotFoundException("Template not found"));

        Worker worker = null;
        if (request.getWorkerId() != null) {
            worker = workerRepo.findByIdAndCompanyIdAndNotArchived(request.getWorkerId(), companyId)
                    .orElseThrow(() -> new WorkerNotFoundException("Worker not found"));
        }

        FormSubmission submission = FormSubmission.builder()
                .company(company)
                .template(template)
                .worker(worker)
                .title(request.getTitle())
                .status(FormSubmissionStatus.DRAFT)
                .build();

        submission = submissionRepo.save(submission);
        return mapToResponse(submission);
    }

    @Override
    public FormSubmissionResponse updateValues(Long submissionId, List<FormFieldValueDto> values, Long actorId,
            boolean isWorker) {
        FormSubmission submission;
        if (isWorker) {
            submission = submissionRepo.findByIdAndWorkerId(submissionId, actorId).orElseThrow();
        } else {
            submission = submissionRepo.findByIdAndCompanyId(submissionId, actorId).orElseThrow();
        }

        for (FormFieldValueDto dto : values) {
            FormField field = fieldRepo.findById(dto.getFieldId()).orElseThrow();

            // --- SECURITY CHECK: ENFORCE ROLE TARGETS ---
            if (isWorker && field.getRoleTarget().name().equals("COMPANY")) {
                throw new InvalidRequestException("Workers are not authorized to edit company-only fields.");
            }
            if (!isWorker && field.getRoleTarget().name().equals("WORKER")) {
                throw new InvalidRequestException("Companies are not authorized to edit worker-only fields.");
            }

            // Upsert value
            FormFieldValue val = submission.getFieldValues().stream()
                    .filter(v -> v.getField().getId().equals(field.getId()))
                    .findFirst().orElse(new FormFieldValue());

            val.setSubmission(submission);
            val.setField(field);
            val.setStringValue(dto.getStringValue());
            val.setBooleanValue(dto.getBooleanValue());
            val.setJsonValue(dto.getJsonValue());

            if (dto.getDateValue() != null) {
                val.setDateValue(LocalDateTime.parse(dto.getDateValue()));
            }

            if (val.getId() == null) {
                submission.getFieldValues().add(val);
            }
        }

        return mapToResponse(submissionRepo.save(submission));
    }

    @Override
    public FormSubmissionResponse uploadFile(Long submissionId, Long fieldId, MultipartFile file, Long actorId,
            boolean isWorker) throws IOException {

        // 1. Basic Validation
        if (file.isEmpty()) {
            throw new EmptyFileException("Uploaded file cannot be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileSizeLimitExceededException("Attachment size must not exceed 10 MB");
        }

        // 2. Detect true file type securely
        String detectedType = tika.detect(file.getInputStream());

        if (blockedTypes.contains(detectedType)) {
            throw new ForbiddenActionException("Upload is blocked for security reasons.");
        }

        // 3. Authorization & Fetching
        FormSubmission submission;
        if (isWorker) {
            submission = submissionRepo.findByIdAndWorkerId(submissionId, actorId).orElseThrow();
        } else {
            submission = submissionRepo.findByIdAndCompanyId(submissionId, actorId).orElseThrow();
        }

        FormField field = fieldRepo.findById(fieldId).orElseThrow();

        // --- SECURITY CHECK: ENFORCE ROLE TARGETS ---
        if (isWorker && field.getRoleTarget().name().equals("COMPANY")) {
            throw new InvalidRequestException("Workers are not authorized to upload to company-only fields.");
        }
        if (!isWorker && field.getRoleTarget().name().equals("WORKER")) {
            throw new InvalidRequestException("Companies are not authorized to upload to worker-only fields.");
        }

        if (field.getType() != FormFieldType.FILE) {
            throw new InvalidRequestException("Field is not a file type");
        }

        String safeFileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        String key = String.format("forms/%d/submissions/%d/%s",
                submission.getCompany().getId(), submissionId, safeFileName);

        s3Service.upload(key, file.getInputStream(), file.getSize(), file.getContentType());

        FormFieldValue val = submission.getFieldValues().stream()
                .filter(v -> v.getField().getId().equals(field.getId()))
                .findFirst().orElse(new FormFieldValue());

        val.setSubmission(submission);
        val.setField(field);
        val.setFileName(file.getOriginalFilename());
        val.setFileType(file.getContentType());
        val.setFileUrl(key);

        if (val.getId() == null) {
            submission.getFieldValues().add(val);
        }

        return mapToResponse(submissionRepo.save(submission));
    }

    @Override
    public void deleteSubmission(Long submissionId, Long companyId) {
        FormSubmission submission = submissionRepo.findByIdAndCompanyId(submissionId, companyId).orElseThrow();

        // Clean up S3
        submission.getFieldValues().stream()
                .filter(v -> v.getField().getType() == FormFieldType.FILE && v.getFileUrl() != null)
                .forEach(v -> s3Service.delete(v.getFileUrl()));

        submissionRepo.delete(submission);
    }

    @Override
    public FormSubmissionResponse sendToWorker(Long submissionId, Long workerId, Long companyId) {
        FormSubmission submission = submissionRepo.findByIdAndCompanyId(submissionId, companyId).orElseThrow();
        Worker worker = workerRepo.findById(workerId).orElseThrow();

        // --- VALIDATION: ENFORCE COMPANY REQUIRED FIELDS BEFORE SENDING ---
        for (FormField field : submission.getTemplate().getFields()) {

            // If the field is required AND only the company can edit it,
            // the company MUST fill it out before passing it to the worker.
            if (field.isRequired() && field.getRoleTarget().name().equals("COMPANY")) {

                boolean hasValue = submission.getFieldValues().stream()
                        .anyMatch(v -> v.getField().getId().equals(field.getId()) &&
                                (v.getStringValue() != null && !v.getStringValue().trim().isEmpty() ||
                                        v.getBooleanValue() != null ||
                                        v.getDateValue() != null ||
                                        v.getJsonValue() != null ||
                                        v.getFileUrl() != null));

                if (!hasValue) {
                    throw new InvalidRequestException(
                            "Cannot send to worker. Required company field missing: '" + field.getLabel() + "'.");
                }
            }
        }

        submission.setWorker(worker);
        submission.setStatus(FormSubmissionStatus.SENT);
        return mapToResponse(submissionRepo.save(submission));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FormSubmissionResponse> getCompanySubmissions(Long companyId) {
        return submissionRepo.findByCompanyId(companyId)
                .stream()
                .map(this::mapToResponse) // Reuses your existing private helper method
                .collect(Collectors.toList());
    }

    @Override
    public List<FormSubmissionResponse> getAssignedForms(Long workerId) {
        return submissionRepo.findByWorkerId(workerId).stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public FormSubmissionResponse markInProgress(Long submissionId, Long workerId) {
        FormSubmission submission = submissionRepo.findByIdAndWorkerId(submissionId, workerId).orElseThrow();
        if (submission.getStatus() == FormSubmissionStatus.SENT) {
            submission.setStatus(FormSubmissionStatus.IN_PROGRESS);
        }
        return mapToResponse(submissionRepo.save(submission));
    }

    @Override
    public FormSubmissionResponse submitForm(Long submissionId, Long workerId) {
        FormSubmission submission = submissionRepo.findByIdAndWorkerId(submissionId, workerId).orElseThrow();

        // --- VALIDATION: ENFORCE REQUIRED FIELDS ---
        for (FormField field : submission.getTemplate().getFields()) {

            // Only enforce required rule if the field isn't restricted to COMPANY only
            if (field.isRequired() && !field.getRoleTarget().name().equals("COMPANY")) {

                boolean hasValue = submission.getFieldValues().stream()
                        .anyMatch(v -> v.getField().getId().equals(field.getId()) &&
                                (v.getStringValue() != null && !v.getStringValue().trim().isEmpty() ||
                                        v.getBooleanValue() != null ||
                                        v.getDateValue() != null ||
                                        v.getJsonValue() != null ||
                                        v.getFileUrl() != null));

                if (!hasValue) {
                    throw new InvalidRequestException(
                            "Required field missing: '" + field.getLabel() + "' must be filled out before submitting.");
                }
            }
        }

        submission.setStatus(FormSubmissionStatus.SUBMITTED);
        submission.setSubmittedAt(LocalDateTime.now(ZoneOffset.UTC));
        return mapToResponse(submissionRepo.save(submission));
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateFormPdf(Long submissionId, Long actorId, boolean isWorker) throws Exception {
        FormSubmission submission;

        if (isWorker) {
            submission = submissionRepo.findByIdAndWorkerId(submissionId, actorId)
                    .orElseThrow(() -> new InvalidRequestException("Form submission not found or access denied"));
        } else {
            submission = submissionRepo.findByIdAndCompanyId(submissionId, actorId)
                    .orElseThrow(() -> new InvalidRequestException("Form submission not found or access denied"));
        }

        // Call the PDF Renderer
        return formPdfRenderer.generatePdf(submission);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FormAttachmentResponse> getFormAttachments(Long submissionId, Long actorId, boolean isWorker) {
        FormSubmission submission;

        // 1. Fetch submission with appropriate authorization
        if (isWorker) {
            submission = submissionRepo.findByIdAndWorkerId(submissionId, actorId)
                    .orElseThrow(() -> new InvalidRequestException("Form submission not found or access denied"));
        } else {
            submission = submissionRepo.findByIdAndCompanyId(submissionId, actorId)
                    .orElseThrow(() -> new InvalidRequestException("Form submission not found or access denied"));
        }

        // 2. Filter out only FILE fields that actually have an uploaded file, then map
        // to DTO
        return submission.getFieldValues().stream()
                .filter(v -> v.getField().getType() == FormFieldType.FILE && v.getFileUrl() != null)
                .map(v -> FormAttachmentResponse.builder()
                        .fieldId(v.getField().getId())
                        .fieldLabel(v.getField().getLabel())
                        .fileName(v.getFileName())
                        .fileType(v.getFileType())
                        .fileUrl(s3Service.resolveFileUrl(v.getFileUrl())) // Generate presigned URL or public URL
                        .build())
                .collect(Collectors.toList());
    }

    private FormSubmissionResponse mapToResponse(FormSubmission s) {
        return FormSubmissionResponse.builder()
                .id(s.getId())
                .title(s.getTitle())
                .status(s.getStatus().name())
                .templateId(s.getTemplate().getId())
                .templateName(s.getTemplate().getName())
                .workerId(s.getWorker() != null ? s.getWorker().getId() : null)
                .workerName(s.getWorker() != null ? s.getWorker().getName() : null)

                // 🚨 THE FIX: Loop over the TEMPLATE fields, not the saved values! 🚨
                .values(s.getTemplate().getFields().stream().map(field -> {
                    // Check if the user has already saved a value for this field
                    FormFieldValue val = s.getFieldValues().stream()
                            .filter(v -> v.getField().getId().equals(field.getId()))
                            .findFirst()
                            .orElse(null);

                    // Safely extract the value depending on its type
                    Object displayValue = null;
                    if (val != null) {
                        if (val.getStringValue() != null)
                            displayValue = val.getStringValue();
                        else if (val.getBooleanValue() != null)
                            displayValue = val.getBooleanValue();
                        else if (val.getDateValue() != null)
                            displayValue = val.getDateValue().toString();
                        else if (val.getJsonValue() != null)
                            displayValue = val.getJsonValue();
                    }

                    return FormFieldValueResponse.builder()
                            .id(val != null ? val.getId() : null)
                            .fieldId(field.getId())
                            .fieldName(field.getName())
                            .fieldLabel(field.getLabel())
                            .fieldType(field.getType().name())
                            .roleTarget(field.getRoleTarget().name())
                            .required(field.isRequired())
                            .value(displayValue) // Now sends null if unanswered, instead of hiding the field!
                            .fileUrl(
                                    val != null && val.getFileUrl() != null ? s3Service.resolveFileUrl(val.getFileUrl())
                                            : null)
                            .fileName(val != null ? val.getFileName() : null)
                            .options(field.getOptions())
                            .build();
                }).collect(Collectors.toList()))
                .build();
    }
}