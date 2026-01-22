package com.workflow.service.jobtemplate;

import com.workflow.common.exception.business.CompanyNotFoundException;
import com.workflow.common.exception.business.DefaultTemplateDeletionException;
import com.workflow.common.exception.business.DuplicateNameException;
import com.workflow.common.exception.business.FieldNotFoundException;
import com.workflow.common.exception.business.TemplateNotFoundException;
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
import java.util.Optional;
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

        // Check if this is the first template for the company
        boolean isFirstTemplate = templateRepository.findByCompanyId(companyId).isEmpty();

        // Handle default template logic
        // First template is ALWAYS default, regardless of request
        // For subsequent templates, only set as default if explicitly requested
        boolean shouldBeDefault = isFirstTemplate || (request.getIsDefault() != null && request.getIsDefault());

        if (shouldBeDefault && !isFirstTemplate) {
            // Clear previous default only if this is not the first template
            handleDefaultTemplateChange(companyId, null);
        }

        JobTemplate template = JobTemplate.builder()
                .company(company)
                .name(request.getName())
                .description(request.getDescription())
                .isDefault(shouldBeDefault)
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

        // Handle default template logic
        // Only allow SETTING as default (isDefault: true), not UNSETTING (isDefault: false)
        boolean shouldBeDefault = request.getIsDefault() != null && request.getIsDefault();

        if (shouldBeDefault && !template.isDefault()) {
            // Setting this template as default - clear previous default
            handleDefaultTemplateChange(companyId, templateId);
            template.setDefault(true);
        }
        // Note: If isDefault is false or null, we DON'T change the current default status
        // Users cannot unset the default flag - they can only switch to another template

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

        // Prevent deletion of default template
        if (template.isDefault()) {
            throw new DefaultTemplateDeletionException("Cannot delete the default template. Please set another template as default first.");
        }

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
                .orElseThrow(() -> new FieldNotFoundException("Field not found with ID: " + fieldId));

        return mapToFieldResponse(field);
    }

    @Override
    public JobTemplateFieldResponse updateField(Long fieldId, JobTemplateFieldCreateRequest request, Long companyId) {
        JobTemplateField field = fieldRepository.findById(fieldId)
                .filter(f -> f.getTemplate().getCompany().getId().equals(companyId))
                .orElseThrow(() -> new FieldNotFoundException("Field not found with ID: " + fieldId));

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
                .orElseThrow(() -> new FieldNotFoundException("Field not found with ID: " + fieldId));

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
    // DEFAULT TEMPLATE
    // -------------------------------------------------------------------------

    @Override
    public Optional<JobTemplateResponse> getDefaultTemplate(Long companyId) {
        return templateRepository.findByCompanyIdAndIsDefaultTrue(companyId)
                .map(this::mapToResponse);
    }

    /**
     * Ensures only one template per company is marked as default.
     * Clears the default flag from any existing default template.
     *
     * @param companyId The company ID
     * @param excludeTemplateId Template ID to exclude from clearing (null if creating new)
     */
    private void handleDefaultTemplateChange(Long companyId, Long excludeTemplateId) {
        templateRepository.findByCompanyIdAndIsDefaultTrue(companyId)
                .ifPresent(existingDefault -> {
                    // Only clear if it's a different template
                    if (excludeTemplateId == null || !existingDefault.getId().equals(excludeTemplateId)) {
                        existingDefault.setDefault(false);
                        templateRepository.save(existingDefault);
                    }
                });
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
                .isDefault(template.isDefault())
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
