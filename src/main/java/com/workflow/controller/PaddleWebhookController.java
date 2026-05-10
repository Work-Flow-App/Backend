package com.workflow.controller;

import java.io.IOException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.dto.paddle.PaddleWebhookPayload;
import com.workflow.service.paddle.PaddleWebhookHandler;
import com.workflow.service.paddle.PaddleWebhookVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Slf4j
public class PaddleWebhookController {

    private final PaddleWebhookVerifier verifier;
    private final PaddleWebhookHandler handler;
    private final ObjectMapper objectMapper;

    /**
     * Receives Paddle webhook events.
     *
     * Security model:
     * - HMAC-SHA256 signature verified by PaddleWebhookVerifier before any processing
     * - SecurityException (bad signature) → 401 (Paddle will NOT retry 4xx)
     * - JsonProcessingException (malformed payload) → 200 (no point retrying unparseable data)
     * - Other exceptions propagate as 500 → Paddle WILL retry (correct for transient DB failures)
     */
    @PostMapping(value = "/paddle", consumes = "application/json")
    public ResponseEntity<Void> receivePaddleWebhook(
            @RequestHeader("Paddle-Signature") String signatureHeader,
            @RequestBody byte[] rawBody) {

        try {
            verifier.verify(signatureHeader, rawBody);
        } catch (SecurityException e) {
            log.warn("Paddle webhook signature verification failed: {}", e.getMessage());
            return ResponseEntity.status(401).build();
        }

        PaddleWebhookPayload payload;
        try {
            payload = objectMapper.readValue(rawBody, PaddleWebhookPayload.class);
        } catch (IOException e) {
            log.error("Failed to deserialize Paddle webhook payload: {}", e.getMessage());
            // Return 200 — malformed payload won't be fixed by retrying
            return ResponseEntity.ok().build();
        }

        // Let DB/transient exceptions propagate as 5xx → Paddle retries
        handler.handle(payload);

        return ResponseEntity.ok().build();
    }
}
