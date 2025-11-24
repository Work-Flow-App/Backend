package com.workflow.service.jobtemplate;

import com.workflow.dto.jobtemplate.*;
import java.util.List;

public interface IJobTemplateService {

    // Template CRUD
    JobTemplateResponse createTemplate(JobTemplateCreateRequest request, Long companyId);
    List<JobTemplateResponse> getAllTemplates(Long companyId);
    JobTemplateResponse getTemplate(Long templateId, Long companyId);
    JobTemplateResponse updateTemplate(Long templateId, JobTemplateCreateRequest request, Long companyId);
    void deleteTemplate(Long templateId, Long companyId);

    // Template Fields CRUD
    JobTemplateFieldResponse createTemplateField(JobTemplateFieldCreateRequest request, Long companyId);
    List<JobTemplateFieldResponse> getFieldsByTemplate(Long templateId, Long companyId);
    JobTemplateFieldResponse getField(Long fieldId, Long companyId);
    JobTemplateFieldResponse updateField(Long fieldId, JobTemplateFieldCreateRequest request, Long companyId);
    void deleteField(Long fieldId, Long companyId);

    // Template with fields
    JobTemplateWithFieldsResponse getTemplateWithFields(Long templateId, Long companyId);
}
