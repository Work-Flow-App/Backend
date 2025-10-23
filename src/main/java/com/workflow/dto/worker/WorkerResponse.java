package com.workflow.dto.worker;

import com.workflow.entity.Worker;

import java.time.LocalDateTime;

public record WorkerResponse(
        Long id,
        String name,
        String initials,
        String telephone,
        String mobile,
        String email,
        String username,
        boolean loginLocked,
        boolean archived,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static WorkerResponse fromEntity(Worker worker) {
        return new WorkerResponse(
                worker.getId(),
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