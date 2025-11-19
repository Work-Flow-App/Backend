package com.workflow.service.jobtemplate;

import com.workflow.dto.jobtemplate.*;
import com.workflow.entity.*;
import com.workflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class JobTemplateService implements IJobTemplateService {

    private final JobTemplateRepository templateRepository;
    private final JobTemplateFieldRepository fieldRepository;
    private final CompanyRepository companyRepository;

    @Override
    public JobTemplateResponse createTemplate(JobTemplateCreateRequest request, Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        JobTemplate template = JobTemplate.builder()
                .company(company)
                .name(request.getName())
                .description(request.getDescription())
                .build();
        templateRepository.save(template);

        return mapToResponse(template);
    }

    @Override
    public List<JobTemplateResponse> getAllTemplates(Long companyId) {
        return templateRepository.findByCompanyId(companyId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public JobTemplateFieldResponse createTemplateField(JobTemplateFieldCreateRequest request, Long companyId) {
        JobTemplate template = templateRepository.findById(request.getTemplateId())
                .filter(t -> t.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new RuntimeException("Template not found"));

        JobTemplateField field = JobTemplateField.builder()
                .template(template)
                .name(request.getName())
                .label(request.getLabel())
                .jobFieldType(request.getJobFieldType())
                .required(request.isRequired())
                .options(request.getOptions())
                .orderIndex(request.getOrderIndex())
                .build();
        fieldRepository.save(field);

        return mapToFieldResponse(field);
    }

    private JobTemplateResponse mapToResponse(JobTemplate template) {
        return JobTemplateResponse.builder()
                .id(template.getId())
                .companyId(template.getCompany().getId())
                .name(template.getName())
                .description(template.getDescription())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }

    private JobTemplateFieldResponse mapToFieldResponse(JobTemplateField field) {
        return JobTemplateFieldResponse.builder()
                .id(field.getId())
                .templateId(field.getTemplate().getId())
                .name(field.getName())
                .label(field.getLabel())
                .jobFieldType(field.getJobFieldType())
                .required(field.isRequired())
                .options(field.getOptions())
                .orderIndex(field.getOrderIndex())
                .build();
    }
}
