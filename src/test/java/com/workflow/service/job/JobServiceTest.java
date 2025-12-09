package com.workflow.service.job;

import com.workflow.common.constant.job.JobFieldType;
import com.workflow.common.constant.job.JobStatus;
import com.workflow.common.exception.business.*;
import com.workflow.dto.job.JobCreateRequest;
import com.workflow.dto.job.JobResponse;
import com.workflow.dto.job.JobUpdateRequest;
import com.workflow.entity.*;
import com.workflow.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobFieldValueRepository fieldValueRepository;

    @Mock
    private JobTemplateRepository templateRepository;

    @Mock
    private JobTemplateFieldRepository templateFieldRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private WorkerRepository workerRepository;

    @InjectMocks
    private JobService jobService;

    private Company company;
    private JobTemplate template;
    private JobTemplateField field1;
    private JobTemplateField field2;
    private Client client;
    private Worker worker;
    private Job job;
    private JobFieldValue fieldValue1;
    private JobFieldValue fieldValue2;
    private JobCreateRequest createRequest;
    private JobUpdateRequest updateRequest;

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

        client = Client.builder()
                .id(1L)
                .name("Test Client")
                .company(company)
                .archived(false)
                .build();

        worker = Worker.builder()
                .id(1L)
                .name("Test Worker")
                .company(company)
                .archived(false)
                .build();

        job = Job.builder()
                .id(55L)
                .template(template)
                .company(company)
                .client(client)
                .assignedWorker(worker)
                .status(JobStatus.NEW)
                .archived(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        fieldValue1 = JobFieldValue.builder()
                .id(1L)
                .job(job)
                .field(field1)
                .stringValue("John Doe")
                .build();

        fieldValue2 = JobFieldValue.builder()
                .id(2L)
                .job(job)
                .field(field2)
                .stringValue("Medium")
                .build();

        Map<Long, Object> fieldValues = new HashMap<>();
        fieldValues.put(101L, "John Doe");
        fieldValues.put(102L, "Medium");

        createRequest = JobCreateRequest.builder()
                .templateId(3L)
                .clientId(1L)
                .assignedWorkerId(1L)
                .status(JobStatus.NEW)
                .fieldValues(fieldValues)
                .build();

        updateRequest = JobUpdateRequest.builder()
                .clientId(1L)
                .assignedWorkerId(1L)
                .status(JobStatus.IN_PROGRESS)
                .archived(false)
                .fieldValues(fieldValues)
                .build();
    }

    // ============= createJob Tests =============

    @Test
    void createJob_ShouldCreateJobSuccessfully() {
        // Arrange
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(templateRepository.findById(3L)).thenReturn(Optional.of(template));
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(templateFieldRepository.findByTemplateIdOrderByOrderIndexAsc(3L))
                .thenReturn(Arrays.asList(field1, field2));
        doAnswer(invocation -> {
            Job savedJob = invocation.getArgument(0);
            savedJob.setId(55L);
            return savedJob;
        }).when(jobRepository).save(any(Job.class));
        when(fieldValueRepository.saveAll(anyList())).thenReturn(Arrays.asList(fieldValue1, fieldValue2));
        when(fieldValueRepository.findByJobId(any()))
                .thenReturn(Arrays.asList(fieldValue1, fieldValue2));

        // Act
        JobResponse response = jobService.createJob(createRequest, 1L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(55L);
        assertThat(response.getStatus()).isEqualTo(JobStatus.NEW);
        verify(companyRepository).findById(1L);
        verify(templateRepository).findById(3L);
        verify(clientRepository).findById(1L);
        verify(workerRepository).findById(1L);
        verify(jobRepository).save(any(Job.class));
        verify(fieldValueRepository).saveAll(anyList());
    }

    @Test
    void createJob_ShouldThrowException_WhenCompanyNotFound() {
        // Arrange
        when(companyRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> jobService.createJob(createRequest, 1L))
                .isInstanceOf(CompanyNotFoundException.class)
                .hasMessageContaining("Company not found");

        verify(companyRepository).findById(1L);
        verify(jobRepository, never()).save(any());
    }

    @Test
    void createJob_ShouldThrowException_WhenTemplateNotFound() {
        // Arrange
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(templateRepository.findById(3L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> jobService.createJob(createRequest, 1L))
                .isInstanceOf(TemplateNotFoundException.class)
                .hasMessageContaining("Template not found");

        verify(companyRepository).findById(1L);
        verify(templateRepository).findById(3L);
        verify(jobRepository, never()).save(any());
    }

    @Test
    void createJob_ShouldThrowException_WhenClientNotFound() {
        // Arrange
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(templateRepository.findById(3L)).thenReturn(Optional.of(template));
        when(clientRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> jobService.createJob(createRequest, 1L))
                .isInstanceOf(ClientNotFoundException.class)
                .hasMessageContaining("Client not found");

        verify(companyRepository).findById(1L);
        verify(templateRepository).findById(3L);
        verify(clientRepository).findById(1L);
        verify(jobRepository, never()).save(any());
    }

    @Test
    void createJob_ShouldThrowException_WhenWorkerNotFound() {
        // Arrange
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(templateRepository.findById(3L)).thenReturn(Optional.of(template));
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(workerRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> jobService.createJob(createRequest, 1L))
                .isInstanceOf(WorkerNotFoundException.class)
                .hasMessageContaining("Worker not found");

        verify(companyRepository).findById(1L);
        verify(templateRepository).findById(3L);
        verify(clientRepository).findById(1L);
        verify(workerRepository).findById(1L);
        verify(jobRepository, never()).save(any());
    }

    @Test
    void createJob_ShouldCreateJobWithoutClientAndWorker() {
        // Arrange
        JobCreateRequest requestWithoutClientAndWorker = JobCreateRequest.builder()
                .templateId(3L)
                .clientId(null)
                .assignedWorkerId(null)
                .status(JobStatus.NEW)
                .fieldValues(new HashMap<>())
                .build();

        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(templateRepository.findById(3L)).thenReturn(Optional.of(template));
        when(templateFieldRepository.findByTemplateIdOrderByOrderIndexAsc(3L)).thenReturn(Arrays.asList());
        doAnswer(invocation -> {
            Job savedJob = invocation.getArgument(0);
            savedJob.setId(55L);
            return savedJob;
        }).when(jobRepository).save(any(Job.class));
        when(fieldValueRepository.findByJobId(any())).thenReturn(Arrays.asList());

        // Act
        JobResponse response = jobService.createJob(requestWithoutClientAndWorker, 1L);

        // Assert
        assertThat(response).isNotNull();
        verify(clientRepository, never()).findById(anyLong());
        verify(workerRepository, never()).findById(anyLong());
    }

    @Test
    void createJob_ShouldUseDefaultStatus_WhenStatusIsNull() {
        // Arrange
        JobCreateRequest requestWithoutStatus = JobCreateRequest.builder()
                .templateId(3L)
                .clientId(1L)
                .assignedWorkerId(1L)
                .status(null)
                .fieldValues(new HashMap<>())
                .build();

        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(templateRepository.findById(3L)).thenReturn(Optional.of(template));
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(templateFieldRepository.findByTemplateIdOrderByOrderIndexAsc(3L)).thenReturn(Arrays.asList());
        doAnswer(invocation -> {
            Job savedJob = invocation.getArgument(0);
            assertThat(savedJob.getStatus()).isEqualTo(JobStatus.NEW);
            savedJob.setId(55L);
            return savedJob;
        }).when(jobRepository).save(any(Job.class));
        when(fieldValueRepository.findByJobId(any())).thenReturn(Arrays.asList());

        // Act
        jobService.createJob(requestWithoutStatus, 1L);

        // Assert
        verify(jobRepository).save(any(Job.class));
    }

    // ============= updateJob Tests =============

    @Test
    void updateJob_ShouldUpdateJobSuccessfully() {
        // Arrange
        when(jobRepository.findById(55L)).thenReturn(Optional.of(job));
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(templateFieldRepository.findByTemplateIdOrderByOrderIndexAsc(3L))
                .thenReturn(Arrays.asList(field1, field2));
        when(jobRepository.save(any(Job.class))).thenReturn(job);
        doNothing().when(fieldValueRepository).deleteByJobId(55L);
        when(fieldValueRepository.saveAll(anyList())).thenReturn(Arrays.asList());
        when(fieldValueRepository.findByJobId(55L))
                .thenReturn(Arrays.asList(fieldValue1, fieldValue2));

        // Act
        JobResponse response = jobService.updateJob(55L, updateRequest, 1L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(JobStatus.IN_PROGRESS);
        verify(jobRepository).findById(55L);
        verify(fieldValueRepository).deleteByJobId(55L);
        verify(jobRepository).save(job);
        verifyNoInteractions(companyRepository);
    }

    @Test
    void updateJob_ShouldThrowException_WhenJobNotFound() {
        // Arrange
        when(jobRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> jobService.updateJob(99L, updateRequest, 1L))
                .isInstanceOf(JobNotFoundException.class)
                .hasMessageContaining("Job not found");

        verify(jobRepository).findById(99L);
        verify(jobRepository, never()).save(any());
    }

    @Test
    void updateJob_ShouldNotUpdateStatus_WhenStatusIsNull() {
        // Arrange
        JobUpdateRequest requestWithoutStatus = JobUpdateRequest.builder()
                .clientId(1L)
                .assignedWorkerId(1L)
                .status(null)
                .archived(false)
                .fieldValues(new HashMap<>())
                .build();

        when(jobRepository.findById(55L)).thenReturn(Optional.of(job));
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(templateFieldRepository.findByTemplateIdOrderByOrderIndexAsc(3L)).thenReturn(Arrays.asList());
        when(jobRepository.save(any(Job.class))).thenReturn(job);
        doNothing().when(fieldValueRepository).deleteByJobId(55L);
        when(fieldValueRepository.findByJobId(55L)).thenReturn(Arrays.asList());

        JobStatus originalStatus = job.getStatus();

        // Act
        jobService.updateJob(55L, requestWithoutStatus, 1L);

        // Assert
        assertThat(job.getStatus()).isEqualTo(originalStatus);
        verify(jobRepository).save(job);
    }

    // ============= getJob Tests =============

    @Test
    void getJob_ShouldReturnJob() {
        // Arrange
        when(jobRepository.findById(55L)).thenReturn(Optional.of(job));
        when(fieldValueRepository.findByJobId(55L))
                .thenReturn(Arrays.asList(fieldValue1, fieldValue2));

        // Act
        JobResponse response = jobService.getJob(55L, 1L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(55L);
        assertThat(response.getStatus()).isEqualTo(JobStatus.NEW);
        verify(jobRepository).findById(55L);
        verify(fieldValueRepository).findByJobId(55L);
        verifyNoInteractions(companyRepository);
    }

    @Test
    void getJob_ShouldThrowException_WhenJobNotFound() {
        // Arrange
        when(jobRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> jobService.getJob(99L, 1L))
                .isInstanceOf(JobNotFoundException.class)
                .hasMessageContaining("Job not found");

        verify(jobRepository).findById(99L);
    }

    // ============= getAllJobs Tests =============

    @Test
    void getAllJobs_ShouldReturnAllJobsForCompany() {
        // Arrange
        Job job2 = Job.builder()
                .id(56L)
                .template(template)
                .company(company)
                .status(JobStatus.IN_PROGRESS)
                .archived(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(jobRepository.findByCompanyId(1L)).thenReturn(Arrays.asList(job, job2));
        when(fieldValueRepository.findByJobId(55L))
                .thenReturn(Arrays.asList(fieldValue1, fieldValue2));
        when(fieldValueRepository.findByJobId(56L)).thenReturn(Arrays.asList());

        // Act
        List<JobResponse> responses = jobService.getAllJobs(1L);

        // Assert
        assertThat(responses).hasSize(2);
        verify(jobRepository).findByCompanyId(1L);
        verifyNoInteractions(companyRepository);
    }

    @Test
    void getAllJobs_ShouldReturnEmptyList_WhenNoJobs() {
        // Arrange
        when(jobRepository.findByCompanyId(1L)).thenReturn(Arrays.asList());

        // Act
        List<JobResponse> responses = jobService.getAllJobs(1L);

        // Assert
        assertThat(responses).isEmpty();
        verify(jobRepository).findByCompanyId(1L);
        verifyNoInteractions(companyRepository);
    }

    // ============= getJobsByTemplate Tests =============

    @Test
    void getJobsByTemplate_ShouldReturnJobsForTemplate() {
        // Arrange
        when(templateRepository.findById(3L)).thenReturn(Optional.of(template));
        when(jobRepository.findByTemplateIdAndCompanyId(3L, 1L))
                .thenReturn(Arrays.asList(job));
        when(fieldValueRepository.findByJobId(55L))
                .thenReturn(Arrays.asList(fieldValue1, fieldValue2));

        // Act
        List<JobResponse> responses = jobService.getJobsByTemplate(3L, 1L);

        // Assert
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getTemplateId()).isEqualTo(3L);
        verify(templateRepository).findById(3L);
        verify(jobRepository).findByTemplateIdAndCompanyId(3L, 1L);
        verifyNoInteractions(companyRepository);
    }

    @Test
    void getJobsByTemplate_ShouldThrowException_WhenTemplateNotFound() {
        // Arrange
        when(templateRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> jobService.getJobsByTemplate(99L, 1L))
                .isInstanceOf(TemplateNotFoundException.class)
                .hasMessageContaining("Template not found");

        verify(templateRepository).findById(99L);
        verify(jobRepository, never()).findByTemplateIdAndCompanyId(anyLong(), anyLong());
    }

    // ============= deleteJob Tests =============

    @Test
    void deleteJob_ShouldDeleteJobSuccessfully() {
        // Arrange
        when(jobRepository.findById(55L)).thenReturn(Optional.of(job));
        doNothing().when(fieldValueRepository).deleteByJobId(55L);
        doNothing().when(jobRepository).delete(any(Job.class));

        // Act
        jobService.deleteJob(55L, 1L);

        // Assert
        verify(jobRepository).findById(55L);
        verify(fieldValueRepository).deleteByJobId(55L);
        verify(jobRepository).delete(job);
        verifyNoInteractions(companyRepository);
    }

    @Test
    void deleteJob_ShouldThrowException_WhenJobNotFound() {
        // Arrange
        when(jobRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> jobService.deleteJob(99L, 1L))
                .isInstanceOf(JobNotFoundException.class)
                .hasMessageContaining("Job not found");

        verify(jobRepository).findById(99L);
        verify(jobRepository, never()).delete(any());
    }
}