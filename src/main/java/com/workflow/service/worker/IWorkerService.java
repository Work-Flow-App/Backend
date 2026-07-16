package com.workflow.service.worker;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.workflow.dto.worker.WorkerCreateRequest;
import com.workflow.dto.worker.WorkerPasswordResetRequest;
import com.workflow.dto.worker.WorkerProfileResponse;
import com.workflow.dto.worker.WorkerRateUpdateRequest;
import com.workflow.dto.worker.WorkerResponse;
import com.workflow.dto.worker.WorkerUpdateRequest;
import com.workflow.dto.worker.WorkerWeeklyHoursResponse;

public interface IWorkerService {
    WorkerResponse createWorker(WorkerCreateRequest request, Long companyUserId);
    List<WorkerResponse> getAllWorkers(Long companyUserId);
    WorkerResponse getWorkerById(Long workerId, Long companyUserId);
    WorkerResponse updateWorker(Long workerId, WorkerUpdateRequest request, Long companyUserId);
    WorkerResponse patchWorker(Long workerId, WorkerUpdateRequest request, Long companyUserId);
    void deleteWorker(Long workerId, Long companyUserId);
    void resetWorkerUsernamePassword(Long workerId, WorkerPasswordResetRequest request, Long companyUserId);

    // Self-service profile
    WorkerProfileResponse getOwnProfile(Long workerUserId);
    WorkerProfileResponse uploadOwnPhoto(Long workerUserId, MultipartFile file) throws IOException;
    WorkerWeeklyHoursResponse getOwnWeeklyHours(Long workerUserId, LocalDate anyDateInWeek);

    // Admin-facing
    WorkerResponse uploadPhotoForWorker(Long workerId, Long companyUserId, MultipartFile file) throws IOException;
    WorkerResponse updateHourlyRate(Long workerId, WorkerRateUpdateRequest request, Long companyUserId);
    WorkerWeeklyHoursResponse getWeeklyHoursForWorker(Long workerId, Long companyUserId, LocalDate anyDateInWeek);
}
