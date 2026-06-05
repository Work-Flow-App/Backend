package com.workflow.dto.worker;

import com.workflow.entity.worker.Worker;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

public record WorkerResponse(
        Long id,
        Long workerRef,
        String name,
        String initials,
        String telephone,
        String mobile,
        String email,
        String username,
        boolean loginLocked,
        boolean archived,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
        LocalDateTime createdAt,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
        LocalDateTime updatedAt
) {
    public static WorkerResponse fromEntity(Worker worker) {
        return new WorkerResponse(
                worker.getId(),
                worker.getWorkerRef(),
                worker.getName(),
                worker.getInitials(),
                worker.getTelephone(),
                worker.getMobile(),
                worker.getEmail(),
                worker.getUser().getUsername(),
                worker.isLoginLocked(),
                worker.isArchived(),
                worker.getCreatedAt(),
                worker.getUpdatedAt()
        );
    }
}
