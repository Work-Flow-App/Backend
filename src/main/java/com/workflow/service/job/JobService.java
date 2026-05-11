package com.workflow.service.job;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workflow.common.constant.job.JobStatus;
import com.workflow.common.exception.business.AssetNotFoundException;
import com.workflow.common.exception.business.ClientNotFoundException;
import com.workflow.common.exception.business.CompanyNotFoundException;
import com.workflow.common.exception.business.CustomerNotFoundException;
import com.workflow.common.exception.business.JobNotFoundException;
import com.workflow.common.exception.business.TemplateNotFoundException;
import com.workflow.common.exception.business.WorkerNotFoundException;
import com.workflow.common.exception.business.InvalidRequestException;
import com.workflow.common.exception.business.WorkflowNotFoundException;
import com.workflow.dto.job.AddressRequest;
import com.workflow.dto.job.AddressResponse;
import com.workflow.dto.job.FieldValueResponse;
import com.workflow.dto.job.JobCreateRequest;
import com.workflow.dto.job.JobResponse;
import com.workflow.dto.job.JobUpdateRequest;
import com.workflow.dto.workflow.JobWorkflowResponse;
import com.workflow.entity.common.Address;
import com.workflow.entity.asset.Asset;
import com.workflow.entity.asset.AssetJobAssignment;
import com.workflow.entity.customer.Client;
import com.workflow.entity.company.Company;
import com.workflow.entity.customer.Customer;
import com.workflow.entity.financial.Estimate;
import com.workflow.entity.financial.Invoice;
import com.workflow.entity.job.Job;
import com.workflow.entity.job.JobFieldValue;
import com.workflow.entity.job.JobTemplate;
import com.workflow.entity.job.JobTemplateField;
import com.workflow.entity.worker.Worker;
import com.workflow.entity.workflow.Workflow;
import com.workflow.repository.common.AddressRepository;
import com.workflow.repository.asset.AssetJobAssignmentRepository;
import com.workflow.repository.asset.AssetRepository;
import com.workflow.repository.customer.ClientRepository;
import com.workflow.repository.company.CompanyRepository;
import com.workflow.repository.customer.CustomerRepository;
import com.workflow.repository.financial.EstimateRepository;
import com.workflow.repository.financial.InvoiceRepository;
import com.workflow.repository.job.JobFieldValueRepository;
import com.workflow.repository.job.JobRepository;
import com.workflow.repository.job.JobTemplateFieldRepository;
import com.workflow.repository.job.JobTemplateRepository;
import com.workflow.repository.worker.WorkerRepository;
import com.workflow.repository.job.JobWorkflowRepository;
import com.workflow.repository.workflow.WorkflowRepository;
import com.workflow.service.sequence.CompanyCounterService;
import com.workflow.service.workflow.IJobWorkflowService;
import com.workflow.util.JsonUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class JobService implements IJobService {

        private final JobRepository jobRepository;
        private final JobFieldValueRepository fieldValueRepository;
        private final JobTemplateRepository templateRepository;
        private final JobTemplateFieldRepository templateFieldRepository;
        private final CompanyRepository companyRepository;
        private final ClientRepository clientRepository;
        private final CustomerRepository customerRepository;
        private final WorkerRepository workerRepository;
        private final AssetRepository assetRepository;
        private final AssetJobAssignmentRepository assetJobAssignmentRepository;
        private final WorkflowRepository workflowRepository;
        private final JobWorkflowRepository jobWorkflowRepository;
        private final EstimateRepository estimateRepository;
        private final InvoiceRepository invoiceRepository;
        private final IJobWorkflowService jobWorkflowService;
        private final AddressRepository addressRepository;
        private final CompanyCounterService companyCounterService;

        @Override
        public JobResponse createJob(JobCreateRequest request, Long companyId) {
                Company company = companyRepository.getReferenceById(companyId);

                JobTemplate template = templateRepository.findById(request.getTemplateId())
                                .filter(t -> t.getCompany().getId().equals(companyId))
                                .orElseThrow(() -> new TemplateNotFoundException("Template not found"));

                Client client = request.getClientId() == null ? null
                                : clientRepository.findById(request.getClientId())
                                                .filter(c -> c.getCompany().getId().equals(companyId))
                                                .orElseThrow(() -> new ClientNotFoundException("Client not found"));

                Customer customer = request.getCustomerId() == null ? null
                                : customerRepository.findById(request.getCustomerId())
                                                .filter(c -> c.getCompany().getId().equals(companyId))
                                                .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));

                Worker worker = request.getAssignedWorkerId() == null ? null
                                : workerRepository.findById(request.getAssignedWorkerId())
                                                .filter(w -> w.getCompany().getId().equals(companyId))
                                                .orElseThrow(() -> new WorkerNotFoundException("Worker not found"));

                Workflow workflow = request.getWorkflowId() == null ? null
                                : workflowRepository.findById(request.getWorkflowId())
                                                .filter(w -> w.getCompany().getId().equals(companyId))
                                                .orElseThrow(() -> new WorkflowNotFoundException("Workflow not found"));

                Address address = null;

                if (request.getAddress() != null) {
                        AddressRequest ar = request.getAddress();

                        address = Address.builder()
                                        .street(ar.getStreet())
                                        .city(ar.getCity())
                                        .state(ar.getState())
                                        .postalCode(ar.getPostalCode())
                                        .country(ar.getCountry())
                                        .additionalInfo(ar.getAdditionalInfo())
                                        .latitude(ar.getLatitude())
                                        .longitude(ar.getLongitude())
                                        .build();

                        addressRepository.save(address);
                }

                long jobRef = companyCounterService.nextJobId(companyId);

                Job job = Job.builder()
                                .company(company)
                                .template(template)
                                .client(client)
                                .customer(customer)
                                .assignedWorker(worker)
                                .workflow(workflow)
                                .address(address)
                                .status(request.getStatus() != null ? request.getStatus() : JobStatus.NEW)
                                .archived(false)
                                .jobRef(jobRef)
                                .build();
                jobRepository.saveAndFlush(job);

                estimateRepository.save(Estimate.builder().job(job).company(company).build());

                saveJobFieldValues(job, request.getFieldValues());
                assignAssetsToJob(job, request.getAssetIds(), companyId);

                if (workflow != null) {
                        JobWorkflowResponse workflowResponse = jobWorkflowService.startWorkflow(job, workflow,
                                        companyId);

                        if (worker != null) {
                                jobWorkflowService.assignAWorkerToAllSteps(
                                                workflowResponse.getId(),
                                                worker.getId(),
                                                companyId);
                        }
                }

                return mapToResponse(job);
        }

        @Override
        public JobResponse updateJob(Long jobId, JobUpdateRequest request, Long companyId) {
                Job job = jobRepository.findById(jobId)
                                .filter(j -> j.getCompany().getId().equals(companyId))
                                .orElseThrow(() -> new JobNotFoundException("Job not found"));

                if (request.getClientId() != null) {
                        Client client = clientRepository.findById(request.getClientId())
                                        .filter(c -> c.getCompany().getId().equals(companyId))
                                        .orElseThrow(() -> new ClientNotFoundException("Client not found"));
                        job.setClient(client);
                }

                if (request.getCustomerId() != null) {
                        Customer customer = customerRepository.findById(request.getCustomerId())
                                        .filter(c -> c.getCompany().getId().equals(companyId))
                                        .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));
                        job.setCustomer(customer);
                }

                if (request.getAssignedWorkerId() != null) {
                        Worker worker = workerRepository.findById(request.getAssignedWorkerId())
                                        .filter(w -> w.getCompany().getId().equals(companyId))
                                        .orElseThrow(() -> new WorkerNotFoundException("Worker not found"));
                        job.setAssignedWorker(worker);
                }

                if (request.getWorkflowId() != null) {
                        if (job.getWorkflow() != null) {
                                throw new IllegalStateException("Workflow cannot be changed once assigned to a job");
                        }
                        Workflow workflow = workflowRepository.findById(request.getWorkflowId())
                                        .filter(w -> w.getCompany().getId().equals(companyId))
                                        .orElseThrow(() -> new WorkflowNotFoundException("Workflow not found"));
                        job.setWorkflow(workflow);
                }

                if (request.getStatus() != null) {
                        job.setStatus(request.getStatus());
                }

                job.setArchived(request.isArchived());

                if (request.getAddress() != null) {
                        AddressRequest ar = request.getAddress();

                        Address address = job.getAddress();

                        if (address == null) {
                                address = new Address();
                        }

                        address.setStreet(ar.getStreet());
                        address.setCity(ar.getCity());
                        address.setState(ar.getState());
                        address.setPostalCode(ar.getPostalCode());
                        address.setCountry(ar.getCountry());
                        address.setAdditionalInfo(ar.getAdditionalInfo());
                        address.setLatitude(ar.getLatitude());
                        address.setLongitude(ar.getLongitude());

                        addressRepository.save(address);
                        job.setAddress(address);
                }

                jobRepository.save(job);

                if (request.getWorkflowId() != null && job.getWorkflow() != null) {
                        jobWorkflowService.startWorkflow(job, job.getWorkflow(), companyId);
                }

                fieldValueRepository.deleteByJobId(jobId);
                saveJobFieldValues(job, request.getFieldValues());

                // Update asset assignments if assetIds is provided
                // null = don't change assets, empty list = remove all assets, list with IDs =
                // replace assets
                if (request.getAssetIds() != null) {
                        updateJobAssetAssignments(job, request.getAssetIds(), companyId);
                }

                return mapToResponse(job);
        }

        private void saveJobFieldValues(Job job, Map<Long, Object> fieldValues) {
                if (fieldValues == null)
                        return;

                List<JobTemplateField> fields = templateFieldRepository
                                .findByTemplateIdOrderByOrderIndexAsc(job.getTemplate().getId());

                List<JobFieldValue> values = fields.stream()
                                .map(f -> {
                                        Object val = fieldValues.get(f.getId());
                                        if (val == null)
                                                return null;

                                        JobFieldValue fieldValue = JobFieldValue.builder()
                                                        .job(job)
                                                        .field(f)
                                                        .build();

                                        switch (f.getJobFieldType()) {
                                                case TEXT, DROPDOWN -> fieldValue.setStringValue(val.toString());
                                                case NUMBER ->
                                                        fieldValue.setNumberValue(Double.valueOf(val.toString()));
                                                case BOOLEAN ->
                                                        fieldValue.setBooleanValue(Boolean.valueOf(val.toString()));
                                                case DATE -> {
                                                        if (val instanceof String s) {
                                                                fieldValue.setDateValue(parseDateTime(s));
                                                        } else if (val instanceof LocalDateTime dt) {
                                                                fieldValue.setDateValue(dt);
                                                        }
                                                }
                                                case JSON -> fieldValue.setJsonValue(JsonUtil.toJson(val));
                                                case REFERENCE -> {
                                                        // Expect Map with keys "id" and "type"
                                                        if (val instanceof Map<?, ?> map) {
                                                                fieldValue.setReferenceId(
                                                                                Long.valueOf(map.get("id").toString()));
                                                                fieldValue.setReferenceType(map.get("type").toString());
                                                        }
                                                }
                                        }

                                        return fieldValue;
                                })
                                .filter(Objects::nonNull)
                                .toList();

                fieldValueRepository.saveAll(values);
        }

        @Override
        @Transactional(readOnly = true)
        public JobResponse getJob(Long jobId, Long companyId) {
                Job job = jobRepository.findById(jobId)
                                .filter(j -> j.getCompany().getId().equals(companyId))
                                .orElseThrow(() -> new JobNotFoundException("Job not found"));

                return mapToResponse(job);
        }

        @Override
        @Transactional(readOnly = true)
        public List<JobResponse> getAllJobs(Long companyId) {
                List<Job> jobs = jobRepository.findByCompanyId(companyId);
                if (jobs.isEmpty()) return new ArrayList<>();

                List<Long> jobIds = jobs.stream().map(Job::getId).collect(Collectors.toList());

                // Batch-load field values for all jobs in one query
                Map<Long, Map<Long, FieldValueResponse>> fieldValuesByJob = fieldValueRepository
                                .findByJobIdIn(jobIds)
                                .stream()
                                .collect(Collectors.groupingBy(
                                                v -> v.getJob().getId(),
                                                Collectors.toMap(
                                                                v -> v.getField().getId(),
                                                                v -> FieldValueResponse.builder()
                                                                                .name(v.getField().getName())
                                                                                .label(v.getField().getLabel())
                                                                                .type(v.getField().getJobFieldType())
                                                                                .value(v.getTypedValue())
                                                                                .build())));

                // Batch-load active asset assignments for all jobs in one query
                Map<Long, List<Long>> assetIdsByJob = assetJobAssignmentRepository
                                .findByJobIdInAndReturnedAtIsNull(jobIds)
                                .stream()
                                .collect(Collectors.groupingBy(
                                                a -> a.getJob().getId(),
                                                Collectors.mapping(a -> a.getAsset().getId(), Collectors.toList())));

                return jobs.stream()
                                .map(job -> mapToResponse(
                                                job,
                                                fieldValuesByJob.getOrDefault(job.getId(), new HashMap<>()),
                                                assetIdsByJob.getOrDefault(job.getId(), new ArrayList<>())))
                                .collect(Collectors.toList());
        }

        @Override
        @Transactional(readOnly = true)
        public List<JobResponse> getArchivedJobs(Long companyId) {
                List<Job> jobs = jobRepository.findArchivedByCompanyId(companyId);
                if (jobs.isEmpty()) return new ArrayList<>();

                List<Long> jobIds = jobs.stream().map(Job::getId).collect(Collectors.toList());

                Map<Long, Map<Long, FieldValueResponse>> fieldValuesByJob = fieldValueRepository
                                .findByJobIdIn(jobIds)
                                .stream()
                                .collect(Collectors.groupingBy(
                                                v -> v.getJob().getId(),
                                                Collectors.toMap(
                                                                v -> v.getField().getId(),
                                                                v -> FieldValueResponse.builder()
                                                                                .name(v.getField().getName())
                                                                                .label(v.getField().getLabel())
                                                                                .type(v.getField().getJobFieldType())
                                                                                .value(v.getTypedValue())
                                                                                .build())));

                Map<Long, List<Long>> assetIdsByJob = assetJobAssignmentRepository
                                .findByJobIdInAndReturnedAtIsNull(jobIds)
                                .stream()
                                .collect(Collectors.groupingBy(
                                                a -> a.getJob().getId(),
                                                Collectors.mapping(a -> a.getAsset().getId(), Collectors.toList())));

                return jobs.stream()
                                .map(job -> mapToResponse(
                                                job,
                                                fieldValuesByJob.getOrDefault(job.getId(), new HashMap<>()),
                                                assetIdsByJob.getOrDefault(job.getId(), new ArrayList<>())))
                                .collect(Collectors.toList());
        }

        @Override
        @Transactional(readOnly = true)
        public List<JobResponse> getJobsByTemplate(Long templateId, Long companyId) {
                JobTemplate template = templateRepository.findById(templateId)
                                .filter(t -> t.getCompany().getId().equals(companyId))
                                .orElseThrow(() -> new TemplateNotFoundException("Template not found"));

                List<Job> jobs = jobRepository.findByTemplateIdAndCompanyId(templateId, companyId);
                if (jobs.isEmpty()) return new ArrayList<>();

                List<Long> jobIds = jobs.stream().map(Job::getId).collect(Collectors.toList());

                // Batch-load field values for all jobs in one query
                Map<Long, Map<Long, FieldValueResponse>> fieldValuesByJob = fieldValueRepository
                                .findByJobIdIn(jobIds)
                                .stream()
                                .collect(Collectors.groupingBy(
                                                v -> v.getJob().getId(),
                                                Collectors.toMap(
                                                                v -> v.getField().getId(),
                                                                v -> FieldValueResponse.builder()
                                                                                .name(v.getField().getName())
                                                                                .label(v.getField().getLabel())
                                                                                .type(v.getField().getJobFieldType())
                                                                                .value(v.getTypedValue())
                                                                                .build())));

                // Batch-load active asset assignments for all jobs in one query
                Map<Long, List<Long>> assetIdsByJob = assetJobAssignmentRepository
                                .findByJobIdInAndReturnedAtIsNull(jobIds)
                                .stream()
                                .collect(Collectors.groupingBy(
                                                a -> a.getJob().getId(),
                                                Collectors.mapping(a -> a.getAsset().getId(), Collectors.toList())));

                return jobs.stream()
                                .map(job -> mapToResponse(
                                                job,
                                                fieldValuesByJob.getOrDefault(job.getId(), new HashMap<>()),
                                                assetIdsByJob.getOrDefault(job.getId(), new ArrayList<>())))
                                .collect(Collectors.toList());
        }

        @Override
        public void deleteJob(Long jobId, Long companyId) {
                Job job = jobRepository.findById(jobId)
                                .filter(j -> j.getCompany().getId().equals(companyId))
                                .orElseThrow(() -> new JobNotFoundException("Job not found"));

                if (!job.isArchived()) {
                        throw new InvalidRequestException("Job must be archived before it can be deleted");
                }

                // Mark assigned assets as available before bulk-deleting the assignment rows.
                // findByJobIdAndReturnedAtIsNull covers only currently active assignments;
                // historical (returned) rows have no effect on availability.
                List<AssetJobAssignment> activeAssignments =
                                assetJobAssignmentRepository.findByJobIdAndReturnedAtIsNull(jobId);
                if (!activeAssignments.isEmpty()) {
                        List<Asset> assetsToRelease = activeAssignments.stream()
                                        .map(AssetJobAssignment::getAsset)
                                        .collect(Collectors.toList());
                        assetsToRelease.forEach(a -> a.setAvailable(true));
                        assetRepository.saveAll(assetsToRelease);
                }

                // Bulk-delete all asset assignments for this job (RESTRICT FK — must go before job).
                assetJobAssignmentRepository.deleteByJobId(jobId);

                // Bulk-delete invoice line item snapshots and invoices before the job delete
                // triggers ON DELETE CASCADE on estimates. invoices.estimate_id is RESTRICT.
                invoiceRepository.deleteLineItemSnapshotsByJobId(jobId);
                invoiceRepository.deleteByJobId(jobId);

                // Bulk-delete the job_workflow record before the job. The FK on job_workflows.job_id
                // has no ON DELETE clause (RESTRICT). Deleting it here lets DB cascades remove
                // job_workflow_steps and all child rows (activities, attachments, comments,
                // visit_logs) via their ON DELETE CASCADE FKs on job_workflow_steps.
                jobWorkflowRepository.deleteByJobId(jobId);
                fieldValueRepository.deleteByJobId(jobId);
                jobRepository.delete(job);
        }

        @Override
        public void archiveJob(Long jobId, Long companyId) {
                Job job = jobRepository.findById(jobId)
                                .filter(j -> j.getCompany().getId().equals(companyId))
                                .orElseThrow(() -> new JobNotFoundException("Job not found"));

                job.setArchived(true);
                jobRepository.save(job);
        }

        /**
         * Updates asset assignments for a job by returning currently assigned assets
         * and assigning new ones.
         *
         * This method handles three scenarios:
         * 1. Empty list: Returns all current assets and makes them available (if not
         * assigned elsewhere)
         * 2. Same assets: Returns and immediately reassigns the same assets
         * 3. Different assets: Returns current assets, assigns new ones
         *
         * @param job       The job to update assignments for
         * @param assetIds  List of new asset IDs to assign (can be empty to remove all)
         * @param companyId Company ID for validation
         */
        private void updateJobAssetAssignments(Job job, List<Long> assetIds, Long companyId) {
                // Step 1: Return all currently assigned assets from this job
                List<AssetJobAssignment> activeAssignments = assetJobAssignmentRepository
                                .findByJobIdAndReturnedAtIsNull(job.getId());

                if (!activeAssignments.isEmpty()) {
                        LocalDateTime now = LocalDateTime.now();

                        // Collect all asset IDs affected by the return so we can batch-check availability
                        List<Long> returningAssetIds = activeAssignments.stream()
                                        .map(a -> a.getAsset().getId())
                                        .collect(Collectors.toList());

                        // Batch-load all OTHER active assignments for those assets in one query
                        Map<Long, Long> otherActiveAssignmentByAssetId = assetJobAssignmentRepository
                                        .findByAssetIdInAndReturnedAtIsNull(returningAssetIds)
                                        .stream()
                                        .filter(a -> !a.getJob().getId().equals(job.getId()))
                                        .collect(Collectors.toMap(
                                                        a -> a.getAsset().getId(),
                                                        AssetJobAssignment::getId,
                                                        (a, b) -> a)); // keep first if duplicates

                        // Mark all assignments as returned
                        for (AssetJobAssignment assignment : activeAssignments) {
                                assignment.setReturnedAt(now);
                        }
                        assetJobAssignmentRepository.saveAll(activeAssignments);

                        // Step 2: Mark assets as available only if no other active assignments exist
                        List<Asset> assetsToMarkAvailable = new ArrayList<>();
                        for (AssetJobAssignment assignment : activeAssignments) {
                                Asset asset = assignment.getAsset();
                                if (!otherActiveAssignmentByAssetId.containsKey(asset.getId())) {
                                        asset.setAvailable(true);
                                        assetsToMarkAvailable.add(asset);
                                }
                        }
                        if (!assetsToMarkAvailable.isEmpty()) {
                                assetRepository.saveAll(assetsToMarkAvailable);
                        }
                }

                // Step 3: Assign new assets (if list is empty, no assets will be assigned)
                assignAssetsToJob(job, assetIds, companyId);
        }

        /**
         * Assigns assets to a job by creating assignment records and marking assets as
         * unavailable.
         * Validates that assets exist, belong to the company, are not archived, and are
         * available.
         *
         * @param job       The job to assign assets to
         * @param assetIds  List of asset IDs to assign
         * @param companyId Company ID for validation
         * @throws AssetNotFoundException if asset not found or doesn't belong to
         *                                company
         * @throws IllegalStateException  if asset is archived or not available
         */
        private void assignAssetsToJob(Job job, List<Long> assetIds, Long companyId) {
                if (assetIds == null || assetIds.isEmpty()) {
                        return;
                }

                // Batch-load all requested assets in one query
                Map<Long, Asset> assetMap = assetRepository.findAllById(assetIds)
                                .stream()
                                .collect(Collectors.toMap(Asset::getId, a -> a));

                // Batch-load all active assignments for these assets in one query (for conflict
                // messages)
                Map<Long, AssetJobAssignment> activeAssignmentByAssetId = assetJobAssignmentRepository
                                .findByAssetIdInAndReturnedAtIsNull(assetIds)
                                .stream()
                                .collect(Collectors.toMap(
                                                a -> a.getAsset().getId(),
                                                a -> a,
                                                (a, b) -> a));

                LocalDateTime now = LocalDateTime.now();
                List<AssetJobAssignment> newAssignments = new ArrayList<>();

                for (Long assetId : assetIds) {
                        Asset asset = assetMap.get(assetId);
                        if (asset == null || !asset.getCompany().getId().equals(companyId)) {
                                throw new AssetNotFoundException("Asset not found with id: " + assetId);
                        }

                        if (asset.isArchived()) {
                                throw new IllegalStateException("Cannot assign archived asset: " + assetId);
                        }

                        if (!asset.isAvailable()) {
                                AssetJobAssignment activeAssignment = activeAssignmentByAssetId.get(assetId);
                                if (activeAssignment != null) {
                                        throw new IllegalStateException(
                                                        "Asset " + assetId + " is already assigned to job "
                                                                        + activeAssignment.getJob().getId()
                                                                        + ". It must be returned before it can be assigned to another job.");
                                }
                                throw new IllegalStateException("Asset is not available: " + assetId);
                        }

                        newAssignments.add(AssetJobAssignment.builder()
                                        .asset(asset)
                                        .job(job)
                                        .assignedAt(now)
                                        .build());

                        asset.setAvailable(false);
                }

                assetJobAssignmentRepository.saveAll(newAssignments);
                // Save all assets whose available flag was just set to false
                assetRepository.saveAll(new ArrayList<>(assetMap.values()));
        }

        /**
         * Parses a date string that can be either:
         * - Date only: "2024-01-15" -> converted to LocalDateTime with time 00:00:00
         * - Date and time: "2024-01-15T10:30:00" -> parsed as-is
         *
         * @param dateString The date string to parse
         * @return LocalDateTime representation
         * @throws IllegalArgumentException if the string cannot be parsed
         */
        private LocalDateTime parseDateTime(String dateString) {
                try {
                        // First try to parse as full LocalDateTime (with time)
                        return LocalDateTime.parse(dateString);
                } catch (DateTimeParseException e) {
                        try {
                                // If that fails, try parsing as LocalDate and convert to LocalDateTime at start
                                // of day
                                LocalDate date = LocalDate.parse(dateString);
                                return date.atStartOfDay();
                        } catch (DateTimeParseException ex) {
                                throw new IllegalArgumentException(
                                                "Invalid date format. Expected ISO date (yyyy-MM-dd) or ISO date-time (yyyy-MM-ddTHH:mm:ss). Got: "
                                                                + dateString);
                        }
                }
        }

        private JobResponse mapToResponse(Job job) {
                Map<Long, FieldValueResponse> values = fieldValueRepository.findByJobId(job.getId())
                                .stream()
                                .collect(Collectors.toMap(
                                                v -> v.getField().getId(),
                                                v -> FieldValueResponse.builder()
                                                                .name(v.getField().getName())
                                                                .label(v.getField().getLabel())
                                                                .type(v.getField().getJobFieldType())
                                                                .value(v.getTypedValue())
                                                                .build()));

                List<Long> assetIds = assetJobAssignmentRepository.findByJobIdAndReturnedAtIsNull(job.getId())
                                .stream()
                                .map(assignment -> assignment.getAsset().getId())
                                .collect(Collectors.toList());

                return mapToResponse(job, values, assetIds);
        }

        private JobResponse mapToResponse(Job job, Map<Long, FieldValueResponse> values, List<Long> assetIds) {

                AddressResponse addressResponse = null;

                if (job.getAddress() != null) {
                        Address a = job.getAddress();
                        addressResponse = AddressResponse.builder()
                                        .id(a.getId())
                                        .street(a.getStreet())
                                        .city(a.getCity())
                                        .state(a.getState())
                                        .postalCode(a.getPostalCode())
                                        .country(a.getCountry())
                                        .additionalInfo(a.getAdditionalInfo())
                                        .latitude(a.getLatitude())
                                        .longitude(a.getLongitude())
                                        .build();
                }

                return JobResponse.builder()
                                .id(job.getId())
                                .jobRef(job.getJobRef())
                                .companyId(job.getCompany().getId())
                                .templateId(job.getTemplate().getId())
                                .clientId(job.getClient() != null ? job.getClient().getId() : null)
                                .customerId(job.getCustomer() != null ? job.getCustomer().getId() : null)
                                .assignedWorkerId(job.getAssignedWorker() != null ? job.getAssignedWorker().getId()
                                                : null)
                                .workflowId(job.getWorkflow() != null ? job.getWorkflow().getId() : null)
                                .status(job.getStatus())
                                .archived(job.isArchived())
                                .createdAt(job.getCreatedAt())
                                .updatedAt(job.getUpdatedAt())
                                .fieldValues(values)
                                .assetIds(assetIds)
                                .address(addressResponse)
                                .build();
        }

}
