package com.workflow.service.job;

import com.workflow.common.constant.PlanType;
import com.workflow.common.constant.job.JobFieldType;
import com.workflow.common.constant.job.JobStatus;
import com.workflow.common.exception.business.*;
import com.workflow.dto.job.AddressRequest;
import com.workflow.dto.job.JobCreateRequest;
import com.workflow.dto.job.JobResponse;
import com.workflow.dto.job.JobUpdateRequest;
import com.workflow.entity.common.Address;
import com.workflow.entity.customer.Client;
import com.workflow.entity.company.Company;
import com.workflow.entity.company.CompanySubscription;
import com.workflow.entity.customer.Customer;
import com.workflow.entity.financial.Estimate;
import com.workflow.entity.job.Job;
import com.workflow.entity.job.JobFieldValue;
import com.workflow.entity.job.JobTemplate;
import com.workflow.entity.job.JobTemplateField;
import com.workflow.repository.asset.AssetJobAssignmentRepository;
import com.workflow.repository.asset.AssetRepository;
import com.workflow.repository.company.CompanyRepository;
import com.workflow.repository.company.CompanySubscriptionRepository;
import com.workflow.repository.customer.ClientRepository;
import com.workflow.repository.customer.CustomerRepository;
import com.workflow.repository.financial.EstimateDocumentRepository;
import com.workflow.repository.financial.EstimateRepository;
import com.workflow.repository.financial.InvoiceRepository;
import com.workflow.repository.form.FormSubmissionRepository;
import com.workflow.repository.job.JobFieldValueRepository;
import com.workflow.repository.job.JobRepository;
import com.workflow.repository.job.JobTemplateFieldRepository;
import com.workflow.repository.job.JobTemplateRepository;
import com.workflow.repository.job.JobWorkflowRepository;
import com.workflow.repository.job.JobWorkflowStepRepository;
import com.workflow.repository.common.AddressRepository;
import com.workflow.repository.workflow.WorkflowRepository;
import com.workflow.service.asset.IAssetAssignmentService;
import com.workflow.service.sequence.CompanyCounterService;
import com.workflow.service.subscription.IPlanLimitsService;
import com.workflow.service.workflow.IJobWorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
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

        @Mock
        private IAssetAssignmentService assetAssignmentService;

        @Mock
        private CompanySubscriptionRepository subscriptionRepository;

        @Mock
        private IPlanLimitsService planLimitsService;

        @Mock
        private FormSubmissionRepository formSubmissionRepository;

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
                                .assignedWorkerIds(List.of(1L))
                                .status(JobStatus.NEW)
                                .fieldValues(fieldValues)
                                .build();

                // Permissive default for assertJobCapacity, which now runs first in every
                // createJob()
                // call (fail-closed to FREE-tier defaults per #5 means an unstubbed plan-limits
                // mock
                // would otherwise return 0 and trip the cap on every test). lenient() since
                // tests that
                // exercise the cap itself override these with more specific stubs.
                CompanySubscription defaultSubscription = CompanySubscription.builder().planType(PlanType.PROFESSIONAL)
                                .build();
                lenient().when(subscriptionRepository.findByCompanyIdForUpdate(anyLong()))
                                .thenReturn(Optional.of(defaultSubscription));
                lenient().when(jobRepository.countByCompanyIdAndCreatedAtBetween(anyLong(), any(), any()))
                                .thenReturn(0L);
                // Explicit type witness needed — bare any() is ambiguous now that
                // getEffectiveJobsPerMonth
                // is overloaded on CompanySubscription vs Optional<CompanySubscription>.
                lenient().when(planLimitsService
                                .getEffectiveJobsPerMonth(ArgumentMatchers.<Optional<CompanySubscription>>any()))
                                .thenReturn(200);
        }

        @Test
        void createJob_ShouldCreateJobSuccessfully() {
                when(companyRepository.getReferenceById(1L)).thenReturn(company);
                when(templateRepository.findById(3L)).thenReturn(Optional.of(template));
                when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
                when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
                when(templateFieldRepository.findByTemplateIdOrderByOrderIndexAsc(3L))
                                .thenReturn(Collections.emptyList());
                when(companyCounterService.nextJobId(1L)).thenReturn(1001L); // Fix: mock Job ID sequence generator

                doAnswer(invocation -> {
                        Job job = invocation.getArgument(0);
                        job.setId(55L);
                        return job;
                }).when(jobRepository).saveAndFlush(any(Job.class));

                JobResponse response = jobService.createJob(createRequest, 1L);

                assertThat(response).isNotNull();
                assertThat(response.getId()).isEqualTo(55L);
                assertThat(response.getStatus()).isEqualTo(JobStatus.NEW);
                assertThat(response.getJobRef()).isEqualTo(1001L);

                verify(companyRepository).getReferenceById(1L);
                verify(templateRepository).findById(3L);
                verify(clientRepository).findById(1L);
                verify(customerRepository).findById(1L);
                verify(jobRepository).saveAndFlush(any(Job.class));
                verify(estimateRepository).save(any(Estimate.class)); // Fix: verify automatic estimate creation
        }

        @Test
        void createJob_ShouldUseDefaultStatus_WhenStatusIsNull() {
                createRequest.setStatus(null);
                when(companyRepository.getReferenceById(1L)).thenReturn(company);
                when(templateRepository.findById(3L)).thenReturn(Optional.of(template));
                when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
                when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
                when(templateFieldRepository.findByTemplateIdOrderByOrderIndexAsc(3L))
                                .thenReturn(Collections.emptyList());
                when(companyCounterService.nextJobId(1L)).thenReturn(1002L);

                doAnswer(invocation -> {
                        Job job = invocation.getArgument(0);
                        job.setId(55L);
                        assertThat(job.getStatus()).isEqualTo(JobStatus.NEW);
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
                when(companyCounterService.nextJobId(1L)).thenReturn(1003L);

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

        // ============= Monthly job cap Tests =============

        @Test
        void createJob_ShouldThrowJobLimitExceededException_WhenMonthlyLimitReached() {
                CompanySubscription subscription = CompanySubscription.builder().planType(PlanType.STARTER).build();
                when(subscriptionRepository.findByCompanyIdForUpdate(1L)).thenReturn(Optional.of(subscription));
                when(jobRepository.countByCompanyIdAndCreatedAtBetween(eq(1L), any(), any())).thenReturn(150L);
                when(planLimitsService.getEffectiveJobsPerMonth(Optional.of(subscription))).thenReturn(150);

                assertThatThrownBy(() -> jobService.createJob(createRequest, 1L))
                                .isInstanceOf(JobLimitExceededException.class);

                // Fails fast — none of the downstream lookups should run once the cap check
                // throws
                verify(companyRepository, never()).getReferenceById(any());
                verify(jobRepository, never()).saveAndFlush(any());
        }

        @Test
        void createJob_ShouldSucceed_WhenUnderMonthlyLimit() {
                CompanySubscription subscription = CompanySubscription.builder().planType(PlanType.STARTER).build();
                when(subscriptionRepository.findByCompanyIdForUpdate(1L)).thenReturn(Optional.of(subscription));
                when(jobRepository.countByCompanyIdAndCreatedAtBetween(eq(1L), any(), any())).thenReturn(149L);
                when(planLimitsService.getEffectiveJobsPerMonth(Optional.of(subscription))).thenReturn(150);

                when(companyRepository.getReferenceById(1L)).thenReturn(company);
                when(templateRepository.findById(3L)).thenReturn(Optional.of(template));
                when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
                when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
                when(templateFieldRepository.findByTemplateIdOrderByOrderIndexAsc(3L))
                                .thenReturn(Collections.emptyList());
                when(companyCounterService.nextJobId(1L)).thenReturn(1003L);
                doAnswer(invocation -> {
                        Job job = invocation.getArgument(0);
                        job.setId(56L);
                        return job;
                }).when(jobRepository).saveAndFlush(any(Job.class));

                JobResponse response = jobService.createJob(createRequest, 1L);

                assertThat(response).isNotNull();
                verify(jobRepository).saveAndFlush(any(Job.class));
        }

        // Decision: fail CLOSED to FREE-tier limits (not open/unlimited) when no
        // CompanySubscription
        // exists — this should never legitimately happen, so it's still logged loudly,
        // but the cap
        // check now actually runs against FREE-tier defaults instead of being skipped.
        @Test
        void createJob_ShouldEvaluateAgainstFreeTierDefaults_WhenNoSubscriptionFound() {
                when(subscriptionRepository.findByCompanyIdForUpdate(1L)).thenReturn(Optional.empty());
                when(jobRepository.countByCompanyIdAndCreatedAtBetween(eq(1L), any(), any())).thenReturn(9L);
                when(planLimitsService.getEffectiveJobsPerMonth(Optional.<CompanySubscription>empty())).thenReturn(10);

                when(companyRepository.getReferenceById(1L)).thenReturn(company);
                when(templateRepository.findById(3L)).thenReturn(Optional.of(template));
                when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
                when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
                when(templateFieldRepository.findByTemplateIdOrderByOrderIndexAsc(3L))
                                .thenReturn(Collections.emptyList());
                when(companyCounterService.nextJobId(1L)).thenReturn(1004L);
                doAnswer(invocation -> {
                        Job job = invocation.getArgument(0);
                        job.setId(57L);
                        return job;
                }).when(jobRepository).saveAndFlush(any(Job.class));

                JobResponse response = jobService.createJob(createRequest, 1L);

                assertThat(response).isNotNull();
                verify(planLimitsService).getEffectiveJobsPerMonth(Optional.<CompanySubscription>empty());
        }

        @Test
        void createJob_ShouldThrowJobLimitExceededException_WhenNoSubscriptionAndFreeTierLimitReached() {
                when(subscriptionRepository.findByCompanyIdForUpdate(1L)).thenReturn(Optional.empty());
                when(jobRepository.countByCompanyIdAndCreatedAtBetween(eq(1L), any(), any())).thenReturn(10L);
                when(planLimitsService.getEffectiveJobsPerMonth(Optional.<CompanySubscription>empty())).thenReturn(10);

                assertThatThrownBy(() -> jobService.createJob(createRequest, 1L))
                                .isInstanceOf(JobLimitExceededException.class);

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

                when(formSubmissionRepository.findByJobIdAndCompanyId(jobId, 1L)).thenReturn(Collections.emptyList());

                jobService.deleteJob(jobId, 1L);

                verify(assetJobAssignmentRepository).deleteByJobId(jobId);
                verify(estimateDocumentRepository).deleteByEstimateJobId(jobId); // Fix: verify document deletion
                verify(invoiceRepository).deleteLineItemSnapshotsByJobId(jobId);
                verify(invoiceRepository).deleteByJobId(jobId);
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

                verify(jobRepository, never()).delete(any(Job.class));
        }

        @Test
        void deleteJob_NotFound() {
                Long jobId = 99L;
                when(jobRepository.findById(jobId)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> jobService.deleteJob(jobId, 1L))
                                .isInstanceOf(JobNotFoundException.class);

                verify(jobRepository, never()).delete(any(Job.class));
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

        // ============= patchJob Tests =============

        private Job buildPatchableJob(Long jobId, boolean archived) {
                return Job.builder()
                                .id(jobId)
                                .company(company)
                                .template(template)
                                .status(JobStatus.NEW)
                                .archived(archived)
                                .build();
        }

        @Test
        void patchJob_StatusOnly_DoesNotTouchFieldValuesOrOtherAssociations() {
                Long jobId = 30L;
                Job job = buildPatchableJob(jobId, false);
                when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

                JobUpdateRequest request = JobUpdateRequest.builder().status(JobStatus.IN_PROGRESS).build();

                JobResponse response = jobService.patchJob(jobId, request, 1L);

                assertThat(response.getStatus()).isEqualTo(JobStatus.IN_PROGRESS);
                assertThat(job.isArchived()).isFalse();

                verify(clientRepository, never()).findById(any());
                verify(customerRepository, never()).findById(any());
                verify(workflowRepository, never()).findById(any());
                verify(addressRepository, never()).save(any());
                verify(fieldValueRepository, never()).findByJobIdAndFieldId(any(), any());
                verify(fieldValueRepository, never()).deleteByJobId(any());
                verify(fieldValueRepository, never()).save(any());
                verify(assetAssignmentService, never()).syncJobAssets(any(), any(), any());
        }

        @Test
        void patchJob_ArchivedOmitted_PreservesExistingArchivedState() {
                Long jobId = 31L;
                Job job = buildPatchableJob(jobId, true);
                when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

                JobUpdateRequest request = JobUpdateRequest.builder().status(JobStatus.IN_PROGRESS).build();

                jobService.patchJob(jobId, request, 1L);

                assertThat(job.isArchived()).isTrue();
        }

        @Test
        void patchJob_ArchivedExplicitFalse_UnarchivesJob() {
                Long jobId = 32L;
                Job job = buildPatchableJob(jobId, true);
                when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

                JobUpdateRequest request = JobUpdateRequest.builder().archived(false).build();

                jobService.patchJob(jobId, request, 1L);

                assertThat(job.isArchived()).isFalse();
        }

        @Test
        void patchJob_FieldValues_MergesSingleKey_WithoutTouchingOthers() {
                Long jobId = 33L;
                Job job = buildPatchableJob(jobId, false);
                when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

                JobTemplateField field42 = JobTemplateField.builder()
                                .id(42L).template(template).jobFieldType(JobFieldType.TEXT).build();
                when(templateFieldRepository.findById(42L)).thenReturn(Optional.of(field42));
                when(fieldValueRepository.findByJobIdAndFieldId(jobId, 42L)).thenReturn(Optional.empty());

                Map<Long, Object> fieldValues = new HashMap<>();
                fieldValues.put(42L, "new value");
                JobUpdateRequest request = JobUpdateRequest.builder().fieldValues(fieldValues).build();

                jobService.patchJob(jobId, request, 1L);

                verify(fieldValueRepository, never()).deleteByJobId(any());
                verify(fieldValueRepository).save(argThat(fv -> fv.getStringValue().equals("new value")));
        }

        @Test
        void patchJob_FieldValues_ExplicitNullClearsOnlyThatField() {
                Long jobId = 34L;
                Job job = buildPatchableJob(jobId, false);
                when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

                JobTemplateField field42 = JobTemplateField.builder()
                                .id(42L).template(template).jobFieldType(JobFieldType.TEXT).build();
                JobFieldValue existingRow = JobFieldValue.builder().id(500L).job(job).field(field42)
                                .stringValue("old value").build();
                when(templateFieldRepository.findById(42L)).thenReturn(Optional.of(field42));
                when(fieldValueRepository.findByJobIdAndFieldId(jobId, 42L)).thenReturn(Optional.of(existingRow));

                Map<Long, Object> fieldValues = new HashMap<>();
                fieldValues.put(42L, null);
                JobUpdateRequest request = JobUpdateRequest.builder().fieldValues(fieldValues).build();

                jobService.patchJob(jobId, request, 1L);

                verify(fieldValueRepository).delete(existingRow);
                verify(fieldValueRepository, never()).save(any());
                verify(fieldValueRepository, never()).deleteByJobId(any());
        }

        @Test
        void patchJob_FieldValues_UnknownFieldIdForTemplate_Throws() {
                Long jobId = 35L;
                Job job = buildPatchableJob(jobId, false);
                when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
                when(templateFieldRepository.findById(99L)).thenReturn(Optional.empty());

                Map<Long, Object> fieldValues = new HashMap<>();
                fieldValues.put(99L, "x");
                JobUpdateRequest request = JobUpdateRequest.builder().fieldValues(fieldValues).build();

                assertThatThrownBy(() -> jobService.patchJob(jobId, request, 1L))
                                .isInstanceOf(InvalidRequestException.class);

                verify(fieldValueRepository, never()).save(any());
        }

        @Test
        void patchJob_Address_PartialMergeOnlyOverwritesProvidedSubfields() {
                Long jobId = 36L;
                Address existingAddress = Address.builder().id(7L).street("S1").city("A").state("B").build();
                Job job = buildPatchableJob(jobId, false);
                job.setAddress(existingAddress);
                when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

                AddressRequest addressRequest = AddressRequest.builder().city("NewCity").build();
                JobUpdateRequest request = JobUpdateRequest.builder().address(addressRequest).build();

                jobService.patchJob(jobId, request, 1L);

                assertThat(existingAddress.getCity()).isEqualTo("NewCity");
                assertThat(existingAddress.getState()).isEqualTo("B");
                assertThat(existingAddress.getStreet()).isEqualTo("S1");
        }

        @Test
        void patchJob_AssetIds_DelegatesToSyncJobAssets() {
                Long jobId = 37L;
                Job job = buildPatchableJob(jobId, false);
                when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

                JobUpdateRequest request = JobUpdateRequest.builder().assetIds(List.of(5L, 6L)).build();

                jobService.patchJob(jobId, request, 1L);

                verify(assetAssignmentService).syncJobAssets(jobId, List.of(5L, 6L), 1L);
        }

        @Test
        void patchJob_NotFound_Throws() {
                Long jobId = 99L;
                when(jobRepository.findById(jobId)).thenReturn(Optional.empty());

                JobUpdateRequest request = JobUpdateRequest.builder().status(JobStatus.IN_PROGRESS).build();

                assertThatThrownBy(() -> jobService.patchJob(jobId, request, 1L))
                                .isInstanceOf(JobNotFoundException.class);
        }

        @Test
        void patchJob_WorkflowAlreadyAssigned_Throws() {
                Long jobId = 38L;
                Job job = buildPatchableJob(jobId, false);
                job.setWorkflow(com.workflow.entity.workflow.Workflow.builder().id(9L).build());
                when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

                JobUpdateRequest request = JobUpdateRequest.builder().workflowId(99L).build();

                assertThatThrownBy(() -> jobService.patchJob(jobId, request, 1L))
                                .isInstanceOf(IllegalStateException.class);
        }
}