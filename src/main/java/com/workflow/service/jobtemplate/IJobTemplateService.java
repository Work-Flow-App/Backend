package com.workflow.service.jobtemplate;

import com.workflow.dto.jobtemplate.*;
import java.util.List;

public interface IJobTemplateService {
    JobTemplateResponse createTemplate(JobTemplateCreateRequest request, Long companyId);
    List<JobTemplateResponse> getAllTemplates(Long companyId);
    JobTemplateFieldResponse createTemplateField(JobTemplateFieldCreateRequest request, Long companyId);
}
