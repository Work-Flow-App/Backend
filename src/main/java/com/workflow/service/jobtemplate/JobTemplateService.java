package com.workflow.service.jobtemplate;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workflow.common.exception.customException.DuplicateNameException;
import com.workflow.dto.jobtemplate.JobTemplateCreateRequest;
import com.workflow.dto.jobtemplate.JobTemplateFieldCreateRequest;
import com.workflow.dto.jobtemplate.JobTemplateFieldResponse;
import com.workflow.dto.jobtemplate.JobTemplateResponse;
import com.workflow.entity.Company;
import com.workflow.entity.JobTemplate;
import com.workflow.entity.JobTemplateField;
import com.workflow.repository.CompanyRepository;
import com.workflow.repository.JobTemplateFieldRepository;
import com.workflow.repository.JobTemplateRepository;

import lombok.RequiredArgsConstructor;

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
        
        if (templateRepository.existsByCompanyIdAndName(companyId, request.getName())) {
                throw new DuplicateNameException("Template name must be unique within the company");
        }

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
