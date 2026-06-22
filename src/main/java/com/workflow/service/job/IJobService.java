package com.workflow.service.job;

import com.workflow.common.constant.job.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.workflow.dto.job.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface IJobService {
    JobResponse createJob(JobCreateRequest request, Long companyId);

    JobResponse updateJob(Long jobId, JobUpdateRequest request, Long companyId);

    JobResponse getJob(Long jobId, Long companyId);

    List<JobResponse> getAllJobs(Long companyId);

    List<JobResponse> getArchivedJobs(Long companyId);

    List<JobResponse> getJobsByTemplate(Long templateId, Long companyId);

    void deleteJob(Long jobId, Long companyId);

    void archiveJob(Long jobId, Long companyId);

    Page<JobResponse> searchJobs(
            Long companyId,
            String search,
            String customerName,
            String clientName,
            String workflowName,
            String templateName,
            JobStatus status,
            Boolean archived,
            BigDecimal minNet,
            BigDecimal maxNet,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable);
}