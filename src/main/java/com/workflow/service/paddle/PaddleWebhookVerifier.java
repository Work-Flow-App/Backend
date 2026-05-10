package com.workflow.service.paddle;

import com.workflow.config.properties.PaddleConfigProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaddleWebhookVerifier {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final PaddleConfigProperties paddleProps;

    /**
     * Verifies the Paddle-Signature header against the raw request body.
     *
     * Header format: ts=<epoch_seconds>;h1=<hex_hmac>
     *
     * @throws SecurityException if the signature is invalid or the timestamp is too old
     */
    public void verify(String signatureHeader, byte[] rawBody) {
        String ts = null;
        String h1 = null;

        for (String part : signatureHeader.split(";")) {
            part = part.trim();
            if (part.startsWith("ts=")) {
                ts = part.substring(3);
            } else if (part.startsWith("h1=")) {
                h1 = part.substring(3);
            }
        }

        if (ts == null || h1 == null) {
            throw new SecurityException("Paddle webhook signature header is malformed");
        }

        // Timestamp freshness check — prevents replay attacks
        long timestampSeconds;
        try {
            timestampSeconds = Long.parseLong(ts);
        } catch (NumberFormatException e) {
            throw new SecurityException("Paddle webhook timestamp is not a valid long: " + ts);
        }

        long nowSeconds = Instant.now().getEpochSecond();
        long delta = Math.abs(nowSeconds - timestampSeconds);
        if (delta > paddleProps.getWebhookTimestampToleranceSeconds()) {
            throw new SecurityException("Webhook timestamp too old. Delta=" + delta + "s, tolerance="
                    + paddleProps.getWebhookTimestampToleranceSeconds() + "s");
        }

        // Build the signed payload exactly as Paddle specifies: ts + ":" + body
        String payload = ts + ":" + new String(rawBody, StandardCharsets.UTF_8);

        // Compute HMAC-SHA256
        String computed;
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKey = new SecretKeySpec(
                    paddleProps.getWebhookSecret().getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM);
            mac.init(secretKey);
            byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            computed = bytesToHex(hmacBytes);
        } catch (Exception e) {
            log.error("HMAC computation failed", e);
            throw new SecurityException("Paddle webhook signature verification failed");
        }

        // Constant-time comparison — prevent timing attacks
        if (!constantTimeEquals(computed, h1)) {
            // Do NOT log any part of the webhook secret or computed HMAC — both are security-sensitive.
            // Log enough to diagnose misconfiguration without leaking key material.
            log.debug("Sig mismatch. payload_prefix={}", payload.substring(0, Math.min(40, payload.length())));
            throw new SecurityException("Paddle webhook signature verification failed");
        }
    }

    /**
     * Converts a byte array to its lowercase hex string representation.
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Constant-time string comparison using XOR to prevent timing side-channel attacks.
     * Returns true only if both strings are equal.
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
