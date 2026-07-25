package com.workflow.service.form;

import com.workflow.dto.form.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

public interface IFormService {
    // Template Management
    FormTemplateRequest createTemplate(FormTemplateRequest request, Long companyId);

    List<FormTemplateRequest> getTemplates(Long companyId);

    FormTemplateRequest updateTemplate(Long templateId, FormTemplateRequest request, Long companyId);

    void deleteTemplate(Long templateId, Long companyId);

    // Form Submission (Company)
    FormSubmissionResponse createDraft(FormSubmissionCreateRequest request, Long companyId);

    FormSubmissionResponse sendToWorker(Long submissionId, Long workerId, Long companyId);

    void deleteSubmission(Long submissionId, Long companyId);

    List<FormSubmissionResponse> getCompanySubmissions(Long companyId);

    // Worker Actions
    List<FormSubmissionResponse> getAssignedForms(Long workerId);

    FormSubmissionResponse markInProgress(Long submissionId, Long workerId);

    FormSubmissionResponse submitForm(Long submissionId, Long workerId);

    // Common
    byte[] generateFormPdf(Long submissionId, Long actorId, boolean isWorker) throws Exception;

    FormSubmissionResponse uploadFile(Long submissionId, Long fieldId, MultipartFile file, Long userId,
            boolean isWorker) throws IOException;

    FormSubmissionResponse updateValues(Long submissionId, List<FormFieldValueDto> values, Long companyId,
            boolean isWorker);

}