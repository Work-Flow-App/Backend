package com.workflow.service.job;

import com.workflow.common.constant.job.JobStatus;
import com.workflow.common.exception.business.*;
import com.workflow.dto.job.JobCreateRequest;
import com.workflow.dto.job.JobResponse;
import com.workflow.entity.customer.Client;
import com.workflow.entity.company.Company;
import com.workflow.entity.customer.Customer;
import com.workflow.entity.job.Job;
import com.workflow.entity.job.JobTemplate;
import com.workflow.repository.asset.AssetJobAssignmentRepository;
import com.workflow.repository.asset.AssetRepository;
import com.workflow.repository.company.CompanyRepository;
import com.workflow.repository.customer.ClientRepository;
import com.workflow.repository.customer.CustomerRepository;
import com.workflow.repository.financial.EstimateDocumentRepository;
import com.workflow.repository.financial.EstimateRepository;
import com.workflow.repository.financial.InvoiceRepository;
import com.workflow.repository.job.JobFieldValueRepository;
import com.workflow.repository.job.JobRepository;
import com.workflow.repository.job.JobTemplateFieldRepository;
import com.workflow.repository.job.JobTemplateRepository;
import com.workflow.repository.job.JobWorkflowRepository;
import com.workflow.repository.job.JobWorkflowStepRepository;
import com.workflow.repository.common.AddressRepository;
import com.workflow.repository.workflow.WorkflowRepository;
import com.workflow.service.sequence.CompanyCounterService;
import com.workflow.service.workflow.IJobWorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        private CustomerRepository customerRepository;

        @Mock
        private AssetRepository assetRepository;

        @Mock
        private AssetJobAssignmentRepository assetJobAssignmentRepository;

        @Mock
        private WorkflowRepository workflowRepository;

        @Mock
        private EstimateDocumentRepository estimateDocumentRepository;

        @Mock
        private EstimateRepository estimateRepository;

        @Mock
        private InvoiceRepository invoiceRepository;

        @Mock
        private JobWorkflowRepository jobWorkflowRepository;

        @Mock
        private JobWorkflowStepRepository jobWorkflowStepRepository;

        @Mock
        private AddressRepository addressRepository;

        @Mock
        private IJobWorkflowService jobWorkflowService;

        @Mock
        private CompanyCounterService companyCounterService;

        @InjectMocks
        private JobService jobService;

        private Company company;
        private JobTemplate template;
        private Client client;
        private Customer customer;
        private JobCreateRequest createRequest;

        @BeforeEach
        void setUp() {
                company = Company.builder().id(1L).name("Test Company").build();
                template = JobTemplate.builder().id(3L).company(company).name("Maintenance").build();
                client = Client.builder().id(1L).company(company).name("Test Client").build();
                customer = Customer.builder().id(1L).company(company).name("Test Customer").build();

                Map<Long, Object> fieldValues = new HashMap<>();
                fieldValues.put(101L, "John Doe");

                createRequest = JobCreateRequest.builder()
                                .templateId(3L)
                                .clientId(1L)
                                .customerId(1L)
                                .assignedWorkerIds(List.of(1L)) // Updated to use List of IDs
                                .status(JobStatus.NEW)
                                .fieldValues(fieldValues)
                                .build();
        }

        @Test
        void createJob_ShouldCreateJobSuccessfully() {
                when(companyRepository.getReferenceById(1L)).thenReturn(company);
                when(templateRepository.findById(3L)).thenReturn(Optional.of(template));
                when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
                when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
                when(templateFieldRepository.findByTemplateIdOrderByOrderIndexAsc(3L))
                                .thenReturn(Collections.emptyList());
                doAnswer(invocation -> {
                        Job job = invocation.getArgument(0);
                        job.setId(55L); // simulate database ID assignment
                        return job;
                }).when(jobRepository).saveAndFlush(any(Job.class));

                JobResponse response = jobService.createJob(createRequest, 1L);

                assertThat(response).isNotNull();
                assertThat(response.getId()).isEqualTo(55L);
                assertThat(response.getStatus()).isEqualTo(JobStatus.NEW);

                verify(companyRepository).getReferenceById(1L);
                verify(templateRepository).findById(3L);
                verify(clientRepository).findById(1L);
                verify(customerRepository).findById(1L);
                verify(jobRepository).saveAndFlush(any(Job.class));
        }

        @Test
        void createJob_ShouldUseDefaultStatus_WhenStatusIsNull() {
                createRequest.setStatus(null); // status is null
                when(companyRepository.getReferenceById(1L)).thenReturn(company);
                when(templateRepository.findById(3L)).thenReturn(Optional.of(template));
                when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
                when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
                when(templateFieldRepository.findByTemplateIdOrderByOrderIndexAsc(3L))
                                .thenReturn(Collections.emptyList());

                doAnswer(invocation -> {
                        Job job = invocation.getArgument(0);
                        job.setId(55L);
                        assertThat(job.getStatus()).isEqualTo(JobStatus.NEW); // verify default
                        return job;
                }).when(jobRepository).saveAndFlush(any(Job.class));

                JobResponse response = jobService.createJob(createRequest, 1L);

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo(JobStatus.NEW);

                verify(jobRepository).saveAndFlush(any(Job.class));
        }

        @Test
        void createJob_ShouldCreateJobSuccessfully_WhenCustomerIsNull() {
                createRequest.setCustomerId(null);
                when(companyRepository.getReferenceById(1L)).thenReturn(company);
                when(templateRepository.findById(3L)).thenReturn(Optional.of(template));
                when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
                when(templateFieldRepository.findByTemplateIdOrderByOrderIndexAsc(3L))
                                .thenReturn(Collections.emptyList());
                doAnswer(invocation -> {
                        Job job = invocation.getArgument(0);
                        job.setId(55L);
                        return job;
                }).when(jobRepository).saveAndFlush(any(Job.class));

                JobResponse response = jobService.createJob(createRequest, 1L);

                assertThat(response).isNotNull();
                assertThat(response.getCustomerId()).isNull();
                verify(customerRepository, never()).findById(any());
        }

        @Test
        void createJob_ShouldThrowException_WhenTemplateNotFound() {
                when(companyRepository.getReferenceById(1L)).thenReturn(company);
                when(templateRepository.findById(3L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> jobService.createJob(createRequest, 1L))
                                .isInstanceOf(TemplateNotFoundException.class)
                                .hasMessageContaining("Template not found");

                verify(jobRepository, never()).saveAndFlush(any());
        }

        @Test
        void createJob_ShouldThrowException_WhenClientNotFound() {
                when(companyRepository.getReferenceById(1L)).thenReturn(company);
                when(templateRepository.findById(3L)).thenReturn(Optional.of(template));
                when(clientRepository.findById(1L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> jobService.createJob(createRequest, 1L))
                                .isInstanceOf(ClientNotFoundException.class)
                                .hasMessageContaining("Client not found");

                verify(jobRepository, never()).saveAndFlush(any());
        }

        // ============= deleteJob Tests =============

        @Test
        void deleteJob_Success() {
                Long jobId = 10L;
                Job archivedJob = Job.builder()
                                .id(jobId)
                                .company(company)
                                .template(template)
                                .status(JobStatus.COMPLETED)
                                .archived(true)
                                .build();

                when(jobRepository.findById(jobId)).thenReturn(Optional.of(archivedJob));

                jobService.deleteJob(jobId, 1L);

                verify(invoiceRepository).deleteLineItemSnapshotsByJobId(jobId);
                verify(invoiceRepository).deleteByJobId(jobId);
                verify(assetJobAssignmentRepository).deleteByJobId(jobId);
                verify(jobWorkflowRepository).deleteByJobId(jobId);
                verify(fieldValueRepository).deleteByJobId(jobId);
                verify(jobRepository).delete(archivedJob);
        }

        @Test
        void deleteJob_ThrowsWhenNotArchived() {
                Long jobId = 11L;
                Job activeJob = Job.builder()
                                .id(jobId)
                                .company(company)
                                .template(template)
                                .status(JobStatus.IN_PROGRESS)
                                .archived(false)
                                .build();

                when(jobRepository.findById(jobId)).thenReturn(Optional.of(activeJob));

                assertThatThrownBy(() -> jobService.deleteJob(jobId, 1L))
                                .isInstanceOf(InvalidRequestException.class)
                                .hasMessageContaining("archived");

                verify(jobRepository, never()).delete(any());
        }

        @Test
        void deleteJob_NotFound() {
                Long jobId = 99L;
                when(jobRepository.findById(jobId)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> jobService.deleteJob(jobId, 1L))
                                .isInstanceOf(JobNotFoundException.class);

                verify(jobRepository, never()).delete(any());
        }

        // ============= archiveJob Tests =============

        @Test
        void archiveJob_Success() {
                Long jobId = 20L;
                Job job = Job.builder()
                                .id(jobId)
                                .company(company)
                                .template(template)
                                .status(JobStatus.NEW)
                                .archived(false)
                                .build();

                when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

                jobService.archiveJob(jobId, 1L);

                assertThat(job.isArchived()).isTrue();
                verify(jobRepository).save(job);
        }

        @Test
        void archiveJob_NotFound() {
                Long jobId = 99L;
                when(jobRepository.findById(jobId)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> jobService.archiveJob(jobId, 1L))
                                .isInstanceOf(JobNotFoundException.class);

                verify(jobRepository, never()).save(any());
        }
}