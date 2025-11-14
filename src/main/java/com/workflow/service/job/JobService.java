package com.workflow.service.job;

import com.workflow.dto.job.*;
import com.workflow.entity.*;
import com.workflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

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
                .orElseThrow(() -> new RuntimeException("Company not found"));

        JobTemplate template = templateRepository.findById(request.getTemplateId())
                .filter(t -> t.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new RuntimeException("Template not found"));

        Client client = request.getClientId() == null ? null :
                clientRepository.findById(request.getClientId())
                        .filter(c -> c.getCompany().getId().equals(companyId))
                        .orElseThrow(() -> new RuntimeException("Client not found"));

        Worker worker = request.getAssignedWorkerId() == null ? null :
                workerRepository.findById(request.getAssignedWorkerId())
                        .filter(w -> w.getCompany().getId().equals(companyId))
                        .orElseThrow(() -> new RuntimeException("Worker not found"));

        Job job = Job.builder()
                .company(company)
                .template(template)
                .client(client)
                .assignedWorker(worker)
                .status(request.getStatus())
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
                .orElseThrow(() -> new RuntimeException("Job not found"));

        if (request.getClientId() != null) {
            Client client = clientRepository.findById(request.getClientId())
                    .filter(c -> c.getCompany().getId().equals(companyId))
                    .orElseThrow(() -> new RuntimeException("Client not found"));
            job.setClient(client);
        }

        if (request.getAssignedWorkerId() != null) {
            Worker worker = workerRepository.findById(request.getAssignedWorkerId())
                    .filter(w -> w.getCompany().getId().equals(companyId))
                    .orElseThrow(() -> new RuntimeException("Worker not found"));
            job.setAssignedWorker(worker);
        }

        job.setStatus(request.getStatus());
        job.setArchived(request.isArchived());
        jobRepository.save(job);

        fieldValueRepository.deleteByJobId(jobId);
        saveJobFieldValues(job, request.getFieldValues());

        return mapToResponse(job);
    }

    private void saveJobFieldValues(Job job, Map<Long, String> fieldValues) {
        if (fieldValues == null) return;

        List<JobTemplateField> fields = templateFieldRepository.findByTemplateIdOrderByOrderIndexAsc(
                job.getTemplate().getId()
        );

        List<JobFieldValue> values = fields.stream()
                .map(f -> JobFieldValue.builder()
                        .job(job)
                        .field(f)
                        .value(fieldValues.get(f.getId()))
                        .build())
                .collect(Collectors.toList());

        fieldValueRepository.saveAll(values);
    }

    @Override
    public JobResponse getJob(Long jobId, Long companyId) {
        Job job = jobRepository.findById(jobId)
                .filter(j -> j.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new RuntimeException("Job not found"));

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
    public void deleteJob(Long jobId, Long companyId) {
        Job job = jobRepository.findById(jobId)
                .filter(j -> j.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new RuntimeException("Job not found"));

        fieldValueRepository.deleteByJobId(jobId);
        jobRepository.delete(job);
    }

    private JobResponse mapToResponse(Job job) {
        Map<Long, String> values = fieldValueRepository.findByJobId(job.getId())
                .stream()
                .collect(Collectors.toMap(
                        v -> v.getField().getId(),
                        JobFieldValue::getValue
                ));

        return JobResponse.builder()
                .id(job.getId())
                .companyId(job.getCompany().getId())
                .templateId(job.getTemplate().getId())
                .clientId(job.getClient() != null ? job.getClient().getId() : null)
                .assignedWorkerId(job.getAssignedWorker() != null ? job.getAssignedWorker().getId() : null)
                .status(job.getStatus())
                .archived(job.isArchived())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .fieldValues(values)
                .build();
    }
}
