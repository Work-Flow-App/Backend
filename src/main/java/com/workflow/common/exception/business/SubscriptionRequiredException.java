package com.workflow.common.exception.business;

import com.workflow.common.exception.base.PaymentRequiredException;

/**
 * Thrown when a company's subscription/trial does not allow access.
 * Maps to HTTP 402 Payment Required.
 */
public class SubscriptionRequiredException extends PaymentRequiredException {

    public SubscriptionRequiredException(String message) {
        super(message);
    }
}