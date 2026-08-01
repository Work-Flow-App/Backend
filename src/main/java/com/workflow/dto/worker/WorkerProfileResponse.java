package com.workflow.dto.worker;

import com.workflow.entity.worker.Worker;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Self-service view of a worker's own profile. Deliberately omits hourlyRate -
 * pay rate is admin-managed and not exposed to the worker themselves.
 */
public record WorkerProfileResponse(
        Long id,
        Long workerRef,
        String name,
        String initials,
        String telephone,
        String mobile,
        String email,
        String username,
        String photoUrl,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
        LocalDateTime createdAt,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
        LocalDateTime updatedAt
) {
    public static WorkerProfileResponse fromEntity(Worker worker, String resolvedPhotoUrl) {
        return new WorkerProfileResponse(
                worker.getId(),
                worker.getWorkerRef(),
                worker.getName(),
                worker.getInitials(),
                worker.getTelephone(),
                worker.getMobile(),
                worker.getEmail(),
                worker.getUser().getUsername(),
                resolvedPhotoUrl,
                worker.getCreatedAt(),
                worker.getUpdatedAt()
        );
    }
}
