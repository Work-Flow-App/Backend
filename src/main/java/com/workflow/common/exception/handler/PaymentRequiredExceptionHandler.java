package com.workflow.common.exception.handler;

import com.workflow.common.exception.ErrorResponse;
import com.workflow.common.exception.base.PaymentRequiredException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Handles all PaymentRequiredException subclasses (402 Payment Required) —
 * SubscriptionRequiredException (lapsed subscription/trial), JobLimitExceededException,
 * SeatLimitExceededException, StorageLimitExceededException (plan-tier limit reached).
 */
@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PaymentRequiredExceptionHandler {

    @ExceptionHandler(PaymentRequiredException.class)
    public ResponseEntity<ErrorResponse> handlePaymentRequired(
            PaymentRequiredException ex,
            HttpServletRequest request) {

        ErrorResponse body = new ErrorResponse(
                402,
                "Payment Required",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(402).body(body);
    }
}
