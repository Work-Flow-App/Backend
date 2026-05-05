package com.workflow.repository.company;

import com.workflow.entity.company.PaddleWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaddleWebhookEventRepository extends JpaRepository<PaddleWebhookEvent, String> {
    // existsById(eventId) is inherited from JpaRepository — used for idempotency check in PaddleWebhookHandler
}