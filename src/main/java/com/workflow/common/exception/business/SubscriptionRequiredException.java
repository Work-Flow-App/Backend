package com.workflow.common.exception.business;

/**
 * Thrown when a company's subscription/trial does not allow access.
 * Maps to HTTP 402 Payment Required.
 * Does NOT extend any existing base exception — 402 has no mapping in the existing hierarchy.
 */
public class SubscriptionRequiredException extends RuntimeException {

    public SubscriptionRequiredException(String message) {
        super(message);
    }
}