package com.workflow.service.jobtemplate;

import com.workflow.common.constant.job.JobFieldType;
import com.workflow.common.exception.business.CompanyNotFoundException;
import com.workflow.common.exception.business.DuplicateNameException;
import com.workflow.common.exception.business.TemplateNotFoundException;
import com.workflow.dto.jobtemplate.*;
import com.workflow.entity.Company;
import com.workflow.entity.JobTemplate;
import com.workflow.entity.JobTemplateField;
import com.workflow.repository.CompanyRepository;
import com.workflow.repository.JobTemplateFieldRepository;
import com.workflow.repository.JobTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobTemplateServiceTest {

    @Mock
    private JobTemplateRepository templateRepository;

    @Mock
    private JobTemplateFieldRepository fieldRepository;

    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private JobTemplateService jobTemplateService;

    private Company company;
    private JobTemplate template;
    private JobTemplateField field1;
    private JobTemplateField field2;
    private JobTemplateCreateRequest createTemplateRequest;
    private JobTemplateFieldCreateRequest createFieldRequest;

    @BeforeEach
    void setUp() {
        company = Company.builder()
                .id(1L)
                .name("Test Company")
                .build();

        template = JobTemplate.builder()
                .id(3L)
                .name("Maintenance Job")
                .description("Standard maintenance template")
                .company(company)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        field1 = JobTemplateField.builder()
                .id(101L)
                .template(template)
                .name("customerName")
                .label("Customer Name")
                .jobFieldType(JobFieldType.TEXT)
                .required(true)
                .orderIndex(1)
                .build();

        field2 = JobTemplateField.builder()
                .id(102L)
                .template(template)
                .name("priority")
                .label("Priority Level")
                .jobFieldType(JobFieldType.DROPDOWN)
                .required(true)
                .options("[\"Low\", \"Medium\", \"High\"]")
                .orderIndex(2)
                .build();

        createTemplateRequest = new JobTemplateCreateRequest(
                "New Template",
                "New template description"
        );

        createFieldRequest = new JobTemplateFieldCreateRequest(
                3L,
                "newField",
                "New Field",
                JobFieldType.TEXT,
                true,
                null,
                1
        );
    }

    // ============= createTemplate Tests =============

    @Test
    void createTemplate_ShouldCreateTemplateSuccessfully() {
        // Arrange
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(templateRepository.existsByCompanyIdAndName(1L, "New Template")).thenReturn(false);
        when(templateRepository.save(any(JobTemplate.class))).thenReturn(template);

        // Act
        JobTemplateResponse response = jobTemplateService.createTemplate(createTemplateRequest, 1L);

        // Assert
        assertThat(response).isNotNull();
        verify(companyRepository).findById(1L);
        verify(templateRepository).existsByCompanyIdAndName(1L, "New Template");
        verify(templateRepository).save(any(JobTemplate.class));
    }

    @Test
    void createTemplate_ShouldThrowException_WhenCompanyNotFound() {
        // Arrange
        when(companyRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> jobTemplateService.createTemplate(createTemplateRequest, 1L))
                .isInstanceOf(CompanyNotFoundException.class)
                .hasMessageContaining("Company not found");

        verify(companyRepository).findById(1L);
        verify(templateRepository, never()).save(any());
    }

    @Test
    void createTemplate_ShouldThrowException_WhenNameAlreadyExists() {
        // Arrange
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(templateRepository.existsByCompanyIdAndName(1L, "New Template")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> jobTemplateService.createTemplate(createTemplateRequest, 1L))
                .isInstanceOf(DuplicateNameException.class)
                .hasMessageContaining("must be unique");

        verify(companyRepository).findById(1L);
        verify(templateRepository).existsByCompanyIdAndName(1L, "New Template");
        verify(templateRepository, never()).save(any());
    }

    // ============= getAllTemplates Tests =============

    @Test
    void getAllTemplates_ShouldReturnAllTemplatesForCompany() {
        // Arrange
        JobTemplate template2 = JobTemplate.builder()
                .id(4L)
                .name("Installation Checklist")
                .company(company)
                .build();

        when(templateRepository.findByCompanyId(1L))
                .thenReturn(Arrays.asList(template, template2));

        // Act
        List<JobTemplateResponse> responses = jobTemplateService.getAllTemplates(1L);

        // Assert
        assertThat(responses).hasSize(2);
        verify(templateRepository).findByCompanyId(1L);
        verifyNoInteractions(companyRepository);
    }

    @Test
    void getAllTemplates_ShouldReturnEmptyList_WhenNoTemplates() {
        // Arrange
        when(templateRepository.findByCompanyId(1L)).thenReturn(Arrays.asList());

        // Act
        List<JobTemplateResponse> responses = jobTemplateService.getAllTemplates(1L);

        // Assert
        assertThat(responses).isEmpty();
        verify(templateRepository).findByCompanyId(1L);
        verifyNoInteractions(companyRepository);
    }

    // ============= getTemplate Tests =============

    @Test
    void getTemplate_ShouldReturnTemplate() {
        // Arrange
        when(templateRepository.findById(3L)).thenReturn(Optional.of(template));

        // Act
        JobTemplateResponse response = jobTemplateService.getTemplate(3L, 1L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(3L);
        assertThat(response.getName()).isEqualTo("Maintenance Job");
        verify(templateRepository).findById(3L);
        verifyNoInteractions(companyRepository);
    }

    @Test
    void getTemplate_ShouldThrowException_WhenTemplateNotFound() {
        // Arrange
        when(templateRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> jobTemplateService.getTemplate(99L, 1L))
                .isInstanceOf(TemplateNotFoundException.class)
                .hasMessageContaining("Template not found");

        verify(templateRepository).findById(99L);
    }

    @Test
    void getTemplate_ShouldThrowException_WhenTemplateBelongsToDifferentCompany() {
        // Arrange
        Company otherCompany = Company.builder()
                .id(2L)
                .name("Other Company")
                .build();
        JobTemplate otherTemplate = JobTemplate.builder()
                .id(5L)
                .company(otherCompany)
                .build();

        when(templateRepository.findById(5L)).thenReturn(Optional.of(otherTemplate));

        // Act & Assert
        assertThatThrownBy(() -> jobTemplateService.getTemplate(5L, 1L))
                .isInstanceOf(TemplateNotFoundException.class)
                .hasMessageContaining("Template not found");

        verify(templateRepository).findById(5L);
    }

    // ============= updateTemplate Tests =============

    @Test
    void updateTemplate_ShouldUpdateTemplateSuccessfully() {
        // Arrange
        JobTemplateCreateRequest updateRequest = new JobTemplateCreateRequest(
                "Updated Template",
                "Updated description"
        );

        when(templateRepository.findById(3L)).thenReturn(Optional.of(template));
        when(templateRepository.existsByCompanyIdAndName(1L, "Updated Template")).thenReturn(false);
        when(templateRepository.save(any(JobTemplate.class))).thenReturn(template);

        // Act
        JobTemplateResponse response = jobTemplateService.updateTemplate(3L, updateRequest, 1L);

        // Assert
        assertThat(response).isNotNull();
        verify(templateRepository).findById(3L);
        verify(templateRepository).existsByCompanyIdAndName(1L, "Updated Template");
        verify(templateRepository).save(template);
        verifyNoInteractions(companyRepository);
    }

    @Test
    void updateTemplate_ShouldThrowException_WhenTemplateNotFound() {
        // Arrange
        when(templateRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> jobTemplateService.updateTemplate(99L, createTemplateRequest, 1L))
                .isInstanceOf(TemplateNotFoundException.class)
                .hasMessageContaining("Template not found");

        verify(templateRepository).findById(99L);
        verify(templateRepository, never()).save(any());
    }

    @Test
    void updateTemplate_ShouldNotCheckNameUniqueness_WhenNameUnchanged() {
        // Arrange
        JobTemplateCreateRequest sameNameRequest = new JobTemplateCreateRequest(
                "Maintenance Job", // Same name as template
                "Updated description"
        );

        when(templateRepository.findById(3L)).thenReturn(Optional.of(template));
        when(templateRepository.save(any(JobTemplate.class))).thenReturn(template);

        // Act
        JobTemplateResponse response = jobTemplateService.updateTemplate(3L, sameNameRequest, 1L);

        // Assert
        assertThat(response).isNotNull();
        verify(templateRepository).findById(3L);
        verify(templateRepository, never()).existsByCompanyIdAndName(anyLong(), anyString());
        verify(templateRepository).save(template);
    }

    // ============= deleteTemplate Tests =============

    @Test
    void deleteTemplate_ShouldDeleteTemplateSuccessfully() {
        // Arrange
        when(templateRepository.findById(3L)).thenReturn(Optional.of(template));
        when(fieldRepository.findByTemplateIdOrderByOrderIndexAsc(3L)).thenReturn(Arrays.asList(field1, field2));
        doNothing().when(fieldRepository).deleteAll(anyList());
        doNothing().when(templateRepository).delete(any(JobTemplate.class));

        // Act
        jobTemplateService.deleteTemplate(3L, 1L);

        // Assert
        verify(templateRepository).findById(3L);
        verify(fieldRepository).findByTemplateIdOrderByOrderIndexAsc(3L);
        verify(fieldRepository).deleteAll(anyList());
        verify(templateRepository).delete(template);
        verifyNoInteractions(companyRepository);
    }

    @Test
    void deleteTemplate_ShouldThrowException_WhenTemplateNotFound() {
        // Arrange
        when(templateRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> jobTemplateService.deleteTemplate(99L, 1L))
                .isInstanceOf(TemplateNotFoundException.class)
                .hasMessageContaining("Template not found");

        verify(templateRepository).findById(99L);
        verify(templateRepository, never()).delete(any());
    }

    // ============= createTemplateField Tests =============

    @Test
    void createTemplateField_ShouldCreateFieldSuccessfully() {
        // Arrange
        when(templateRepository.findById(3L)).thenReturn(Optional.of(template));
        when(fieldRepository.save(any(JobTemplateField.class))).thenReturn(field1);

        // Act
        JobTemplateFieldResponse response = jobTemplateService.createTemplateField(createFieldRequest, 1L);

        // Assert
        assertThat(response).isNotNull();
        verify(templateRepository).findById(3L);
        verify(fieldRepository).save(any(JobTemplateField.class));
        verifyNoInteractions(companyRepository);
    }

    @Test
    void createTemplateField_ShouldThrowException_WhenTemplateNotFound() {
        // Arrange
        when(templateRepository.findById(3L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> jobTemplateService.createTemplateField(createFieldRequest, 1L))
                .isInstanceOf(TemplateNotFoundException.class)
                .hasMessageContaining("Template not found");

        verify(templateRepository).findById(3L);
        verify(fieldRepository, never()).save(any());
    }

    // ============= getFieldsByTemplate Tests =============

    @Test
    void getFieldsByTemplate_ShouldReturnAllFieldsForTemplate() {
        // Arrange
        when(templateRepository.findById(3L)).thenReturn(Optional.of(template));
        when(fieldRepository.findByTemplateIdOrderByOrderIndexAsc(3L))
                .thenReturn(Arrays.asList(field1, field2));

        // Act
        List<JobTemplateFieldResponse> responses = jobTemplateService.getFieldsByTemplate(3L, 1L);

        // Assert
        assertThat(responses).hasSize(2);
        verify(templateRepository).findById(3L);
        verify(fieldRepository).findByTemplateIdOrderByOrderIndexAsc(3L);
        verifyNoInteractions(companyRepository);
    }

    @Test
    void getFieldsByTemplate_ShouldReturnEmptyList_WhenNoFields() {
        // Arrange
        when(templateRepository.findById(3L)).thenReturn(Optional.of(template));
        when(fieldRepository.findByTemplateIdOrderByOrderIndexAsc(3L)).thenReturn(Arrays.asList());

        // Act
        List<JobTemplateFieldResponse> responses = jobTemplateService.getFieldsByTemplate(3L, 1L);

        // Assert
        assertThat(responses).isEmpty();
        verify(templateRepository).findById(3L);
        verify(fieldRepository).findByTemplateIdOrderByOrderIndexAsc(3L);
    }

    // ============= getField Tests =============

    @Test
    void getField_ShouldReturnField() {
        // Arrange
        when(fieldRepository.findById(101L)).thenReturn(Optional.of(field1));

        // Act
        JobTemplateFieldResponse response = jobTemplateService.getField(101L, 1L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(101L);
        assertThat(response.getName()).isEqualTo("customerName");
        verify(fieldRepository).findById(101L);
        verifyNoInteractions(companyRepository);
    }

    @Test
    void getField_ShouldThrowException_WhenFieldNotFound() {
        // Arrange
        when(fieldRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> jobTemplateService.getField(999L, 1L))
                .isInstanceOf(TemplateNotFoundException.class)
                .hasMessageContaining("Field not found");

        verify(fieldRepository).findById(999L);
    }

    // ============= updateField Tests =============

    @Test
    void updateField_ShouldUpdateFieldSuccessfully() {
        // Arrange
        JobTemplateFieldCreateRequest updateRequest = new JobTemplateFieldCreateRequest(
                3L,
                "customerName",
                "Updated Label",
                JobFieldType.TEXT,
                false,
                null,
                1
        );

        when(fieldRepository.findById(101L)).thenReturn(Optional.of(field1));
        when(fieldRepository.save(any(JobTemplateField.class))).thenReturn(field1);

        // Act
        JobTemplateFieldResponse response = jobTemplateService.updateField(101L, updateRequest, 1L);

        // Assert
        assertThat(response).isNotNull();
        verify(fieldRepository).findById(101L);
        verify(fieldRepository).save(field1);
        verifyNoInteractions(companyRepository);
    }

    @Test
    void updateField_ShouldThrowException_WhenFieldNotFound() {
        // Arrange
        when(fieldRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> jobTemplateService.updateField(999L, createFieldRequest, 1L))
                .isInstanceOf(TemplateNotFoundException.class)
                .hasMessageContaining("Field not found");

        verify(fieldRepository).findById(999L);
        verify(fieldRepository, never()).save(any());
    }

    // ============= deleteField Tests =============

    @Test
    void deleteField_ShouldDeleteFieldSuccessfully() {
        // Arrange
        when(fieldRepository.findById(101L)).thenReturn(Optional.of(field1));
        doNothing().when(fieldRepository).delete(any(JobTemplateField.class));

        // Act
        jobTemplateService.deleteField(101L, 1L);

        // Assert
        verify(fieldRepository).findById(101L);
        verify(fieldRepository).delete(field1);
        verifyNoInteractions(companyRepository);
    }

    @Test
    void deleteField_ShouldThrowException_WhenFieldNotFound() {
        // Arrange
        when(fieldRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> jobTemplateService.deleteField(999L, 1L))
                .isInstanceOf(TemplateNotFoundException.class)
                .hasMessageContaining("Field not found");

        verify(fieldRepository).findById(999L);
        verify(fieldRepository, never()).delete(any());
    }

    // ============= getTemplateWithFields Tests =============

    @Test
    void getTemplateWithFields_ShouldReturnTemplateWithFields() {
        // Arrange
        when(templateRepository.findById(3L)).thenReturn(Optional.of(template));
        when(fieldRepository.findByTemplateIdOrderByOrderIndexAsc(3L))
                .thenReturn(Arrays.asList(field1, field2));

        // Act
        JobTemplateWithFieldsResponse response = jobTemplateService.getTemplateWithFields(3L, 1L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getTemplate()).isNotNull();
        assertThat(response.getTemplate().getId()).isEqualTo(3L);
        assertThat(response.getFields()).hasSize(2);
        verify(templateRepository).findById(3L);
        verify(fieldRepository).findByTemplateIdOrderByOrderIndexAsc(3L);
        verifyNoInteractions(companyRepository);
    }

    @Test
    void getTemplateWithFields_ShouldThrowException_WhenTemplateNotFound() {
        // Arrange
        when(templateRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> jobTemplateService.getTemplateWithFields(99L, 1L))
                .isInstanceOf(TemplateNotFoundException.class)
                .hasMessageContaining("Template not found");

        verify(templateRepository).findById(99L);
        verify(fieldRepository, never()).findByTemplateIdOrderByOrderIndexAsc(anyLong());
    }
}