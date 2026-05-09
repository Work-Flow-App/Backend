package com.workflow.dto.paddle;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

public record PaddleWebhookPayload(
        @JsonProperty("notification_id") String notificationId,
        @JsonProperty("event_id") String eventId,
        @JsonProperty("event_type") String eventType,
        @JsonProperty("occurred_at") String occurredAt,
        JsonNode data
) {}