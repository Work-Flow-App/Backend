package com.workflow.common.exception.handler;

import com.workflow.common.exception.ErrorResponse;
import com.workflow.common.exception.business.SubscriptionRequiredException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PaymentRequiredExceptionHandler {

    @ExceptionHandler(SubscriptionRequiredException.class)
    public ResponseEntity<ErrorResponse> handleSubscriptionRequired(
            SubscriptionRequiredException ex,
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
