package com.workflow.common.exception.handler;

import com.workflow.common.exception.ResponseBuilder;
import com.workflow.common.exception.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Catches all unhandled exceptions (fallback handler)
 * Order: LOWEST_PRECEDENCE (processed last)
 *
 * This handler catches any exception not handled by specific handlers.
 * It logs the full exception details but returns a generic message to the client
 * to avoid exposing sensitive information.
 */
@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GeneralExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GeneralExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(
            Exception ex,
            HttpServletRequest request) {

        // Log full exception details for debugging
        log.error("Unhandled exception occurred: {} at path: {}",
                ex.getClass().getSimpleName(),
                request.getRequestURI(),
                ex);

        // Return generic message to client (don't expose internal details)
        return ResponseBuilder.buildInternalServerErrorResponse(
                "An unexpected error occurred. Please contact support if the problem persists.",
                request
        );
    }
}
