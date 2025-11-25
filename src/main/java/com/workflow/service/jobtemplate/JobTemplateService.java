package com.workflow.service.jobtemplate;

import com.workflow.common.exception.customException.CompanyNotFoundException;
import com.workflow.common.exception.customException.DuplicateNameException;
import com.workflow.common.exception.customException.TemplateNotFoundException;
import com.workflow.dto.jobtemplate.*;
import com.workflow.entity.Company;
import com.workflow.entity.JobTemplate;
import com.workflow.entity.JobTemplateField;
import com.workflow.repository.CompanyRepository;
import com.workflow.repository.JobTemplateFieldRepository;
import com.workflow.repository.JobTemplateRepository;
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

    // -------------------------------------------------------------------------
    // JOB TEMPLATE CRUD
    // -------------------------------------------------------------------------

    @Override
    public JobTemplateResponse createTemplate(JobTemplateCreateRequest request, Long companyId) {

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new CompanyNotFoundException("Company not found"));

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
    public JobTemplateResponse getTemplate(Long templateId, Long companyId) {
        JobTemplate template = templateRepository.findById(templateId)
                .filter(t -> t.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new TemplateNotFoundException("Template not found"));
        return mapToResponse(template);
    }

    @Override
    public JobTemplateResponse updateTemplate(Long templateId, JobTemplateCreateRequest request, Long companyId) {
        JobTemplate template = templateRepository.findById(templateId)
                .filter(t -> t.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new TemplateNotFoundException("Template not found"));

        // Validate name uniqueness
        if (!template.getName().equals(request.getName())
                && templateRepository.existsByCompanyIdAndName(companyId, request.getName())) {
            throw new DuplicateNameException("Template name must be unique");
        }

        template.setName(request.getName());
        template.setDescription(request.getDescription());
        templateRepository.save(template);

        return mapToResponse(template);
    }

    @Override
    public void deleteTemplate(Long templateId, Long companyId) {
        JobTemplate template = templateRepository.findById(templateId)
                .filter(t -> t.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new TemplateNotFoundException("Template not found"));

        // Delete associated fields
        fieldRepository.deleteAll(fieldRepository.findByTemplateIdOrderByOrderIndexAsc(templateId));

        templateRepository.delete(template);
    }

    // -------------------------------------------------------------------------
    // TEMPLATE FIELDS CRUD
    // -------------------------------------------------------------------------

    @Override
    public JobTemplateFieldResponse createTemplateField(JobTemplateFieldCreateRequest request, Long companyId) {
        JobTemplate template = templateRepository.findById(request.getTemplateId())
                .filter(t -> t.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new TemplateNotFoundException("Template not found"));

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

    @Override
    public List<JobTemplateFieldResponse> getFieldsByTemplate(Long templateId, Long companyId) {
        JobTemplate template = templateRepository.findById(templateId)
                .filter(t -> t.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new TemplateNotFoundException("Template not found"));

        return fieldRepository.findByTemplateIdOrderByOrderIndexAsc(template.getId())
                .stream()
                .map(this::mapToFieldResponse)
                .collect(Collectors.toList());
    }

    @Override
    public JobTemplateFieldResponse getField(Long fieldId, Long companyId) {
        JobTemplateField field = fieldRepository.findById(fieldId)
                .filter(f -> f.getTemplate().getCompany().getId().equals(companyId))
                .orElseThrow(() -> new TemplateNotFoundException("Field not found"));

        return mapToFieldResponse(field);
    }

    @Override
    public JobTemplateFieldResponse updateField(Long fieldId, JobTemplateFieldCreateRequest request, Long companyId) {
        JobTemplateField field = fieldRepository.findById(fieldId)
                .filter(f -> f.getTemplate().getCompany().getId().equals(companyId))
                .orElseThrow(() -> new TemplateNotFoundException("Field not found"));

        field.setName(request.getName());
        field.setLabel(request.getLabel());
        field.setJobFieldType(request.getJobFieldType());
        field.setRequired(request.isRequired());
        field.setOptions(request.getOptions());
        field.setOrderIndex(request.getOrderIndex());

        fieldRepository.save(field);

        return mapToFieldResponse(field);
    }

    @Override
    public void deleteField(Long fieldId, Long companyId) {
        JobTemplateField field = fieldRepository.findById(fieldId)
                .filter(f -> f.getTemplate().getCompany().getId().equals(companyId))
                .orElseThrow(() -> new TemplateNotFoundException("Field not found"));

        fieldRepository.delete(field);
    }

    // -------------------------------------------------------------------------
    // TEMPLATE + FIELDS
    // -------------------------------------------------------------------------

    @Override
    public JobTemplateWithFieldsResponse getTemplateWithFields(Long templateId, Long companyId) {
        JobTemplate template = templateRepository.findById(templateId)
                .filter(t -> t.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new TemplateNotFoundException("Template not found"));

        List<JobTemplateFieldResponse> fields = fieldRepository
                .findByTemplateIdOrderByOrderIndexAsc(templateId)
                .stream()
                .map(this::mapToFieldResponse)
                .collect(Collectors.toList());

        return JobTemplateWithFieldsResponse.builder()
                .template(mapToResponse(template))
                .fields(fields)
                .build();
    }

    // -------------------------------------------------------------------------
    // MAPPERS
    // -------------------------------------------------------------------------

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
