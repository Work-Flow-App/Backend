package com.workflow.service.job;

import com.workflow.dto.job.*;
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
}
