package com.workflow.service.worker;

import com.workflow.dto.worker.WorkerCreateRequest;
import com.workflow.dto.worker.WorkerResponse;
import com.workflow.dto.worker.WorkerUpdateRequest;

import java.util.List;

public interface IWorkerService {
    WorkerResponse createWorker(WorkerCreateRequest request, Long companyUserId);
    List<WorkerResponse> getAllWorkers(Long companyUserId);
    WorkerResponse getWorkerById(Long workerId, Long companyUserId);
    WorkerResponse updateWorker(Long workerId, WorkerUpdateRequest request, Long companyUserId);
    WorkerResponse patchWorker(Long workerId, WorkerUpdateRequest request, Long companyUserId);
    void deleteWorker(Long workerId, Long companyUserId);
}