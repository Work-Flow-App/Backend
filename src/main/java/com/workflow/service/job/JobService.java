package com.workflow.service.job;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workflow.common.constant.job.JobStatus;
import com.workflow.common.exception.business.ClientNotFoundException;
import com.workflow.common.exception.business.CompanyNotFoundException;
import com.workflow.common.exception.business.JobNotFoundException;
import com.workflow.common.exception.business.TemplateNotFoundException;
import com.workflow.common.exception.business.WorkerNotFoundException;
import com.workflow.dto.job.FieldValueResponse;
import com.workflow.dto.job.JobCreateRequest;
import com.workflow.dto.job.JobResponse;
import com.workflow.dto.job.JobUpdateRequest;
import com.workflow.entity.Client;
import com.workflow.entity.Company;
import com.workflow.entity.Job;
import com.workflow.entity.JobFieldValue;
import com.workflow.entity.JobTemplate;
import com.workflow.entity.JobTemplateField;
import com.workflow.entity.Worker;
import com.workflow.repository.ClientRepository;
import com.workflow.repository.CompanyRepository;
import com.workflow.repository.JobFieldValueRepository;
import com.workflow.repository.JobRepository;
import com.workflow.repository.JobTemplateFieldRepository;
import com.workflow.repository.JobTemplateRepository;
import com.workflow.repository.WorkerRepository;
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
        private final WorkerRepository workerRepository;

        @Override
        public JobResponse createJob(JobCreateRequest request, Long companyId) {
                Company company = companyRepository.findById(companyId)
                                .orElseThrow(() -> new CompanyNotFoundException("Company not found"));

                JobTemplate template = templateRepository.findById(request.getTemplateId())
                                .filter(t -> t.getCompany().getId().equals(companyId))
                                .orElseThrow(() -> new TemplateNotFoundException("Template not found"));

                Client client = request.getClientId() == null ? null
                                : clientRepository.findById(request.getClientId())
                                                .filter(c -> c.getCompany().getId().equals(companyId))
                                                .orElseThrow(() -> new ClientNotFoundException("Client not found"));

                Worker worker = request.getAssignedWorkerId() == null ? null
                                : workerRepository.findById(request.getAssignedWorkerId())
                                                .filter(w -> w.getCompany().getId().equals(companyId))
                                                .orElseThrow(() -> new WorkerNotFoundException("Worker not found"));

                Job job = Job.builder()
                                .company(company)
                                .template(template)
                                .client(client)
                                .assignedWorker(worker)
                                .status(request.getStatus() != null ? request.getStatus() : JobStatus.NEW)
                                .archived(false)
                                .build();
                jobRepository.save(job);

                saveJobFieldValues(job, request.getFieldValues());

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

                if (request.getAssignedWorkerId() != null) {
                        Worker worker = workerRepository.findById(request.getAssignedWorkerId())
                                        .filter(w -> w.getCompany().getId().equals(companyId))
                                        .orElseThrow(() -> new WorkerNotFoundException("Worker not found"));
                        job.setAssignedWorker(worker);
                }

                if (request.getStatus() != null) {
                        job.setStatus(request.getStatus());
                }

                job.setArchived(request.isArchived());
                jobRepository.save(job);

                fieldValueRepository.deleteByJobId(jobId);
                saveJobFieldValues(job, request.getFieldValues());

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
                                                        if (val instanceof String s)
                                                                fieldValue.setDateValue(LocalDateTime.parse(s));
                                                        else if (val instanceof LocalDateTime dt)
                                                                fieldValue.setDateValue(dt);
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
        public JobResponse getJob(Long jobId, Long companyId) {
                Job job = jobRepository.findById(jobId)
                                .filter(j -> j.getCompany().getId().equals(companyId))
                                .orElseThrow(() -> new JobNotFoundException("Job not found"));

                return mapToResponse(job);
        }

        @Override
        public List<JobResponse> getAllJobs(Long companyId) {
                return jobRepository.findByCompanyId(companyId)
                                .stream()
                                .map(this::mapToResponse)
                                .collect(Collectors.toList());
        }

        @Override
        public List<JobResponse> getJobsByTemplate(Long templateId, Long companyId) {
                JobTemplate template = templateRepository.findById(templateId)
                                .filter(t -> t.getCompany().getId().equals(companyId))
                                .orElseThrow(() -> new TemplateNotFoundException("Template not found"));

                return jobRepository.findByTemplateIdAndCompanyId(templateId, companyId)
                                .stream()
                                .map(this::mapToResponse)
                                .collect(Collectors.toList());
        }

        @Override
        public void deleteJob(Long jobId, Long companyId) {
                Job job = jobRepository.findById(jobId)
                                .filter(j -> j.getCompany().getId().equals(companyId))
                                .orElseThrow(() -> new JobNotFoundException("Job not found"));

                fieldValueRepository.deleteByJobId(jobId);
                jobRepository.delete(job);
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
                                                                .build()
                                ));

                return JobResponse.builder()
                                .id(job.getId())
                                .companyId(job.getCompany().getId())
                                .templateId(job.getTemplate().getId())
                                .clientId(job.getClient() != null ? job.getClient().getId() : null)
                                .assignedWorkerId(job.getAssignedWorker() != null ? job.getAssignedWorker().getId()
                                                : null)
                                .status(job.getStatus())
                                .archived(job.isArchived())
                                .createdAt(job.getCreatedAt())
                                .updatedAt(job.getUpdatedAt())
                                .fieldValues(values)
                                .build();
        }

}
