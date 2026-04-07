package com.workflow.common.exception.handler;

import com.workflow.common.exception.ResponseBuilder;
import com.workflow.common.exception.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

/**
 * Handles database-related exceptions such as constraint violations
 * Order: HIGH (processed before general exception handler)
 */
@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class DatabaseExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(DatabaseExceptionHandler.class);

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        log.error("Database constraint violation at {}: {}",
                request.getRequestURI(),
                ex.getMessage());

        String message = extractUserFriendlyMessage(ex);

        return ResponseBuilder.buildConflictResponse(message, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        log.error("Constraint violation at {}: {}",
                request.getRequestURI(),
                ex.getMessage());

        String message = "Validation constraint violated: " + ex.getMessage();

        return ResponseBuilder.buildBadRequestResponse(message, request);
    }

    /**
     * Extracts a user-friendly error message from database exceptions
     */
    private String extractUserFriendlyMessage(DataIntegrityViolationException ex) {
        String exceptionMessage = ex.getMessage();
        String rootCauseMessage = ex.getRootCause() != null ? ex.getRootCause().getMessage() : "";

        // Check for common constraint violations
        if (exceptionMessage != null || rootCauseMessage != null) {
            String combinedMessage = (exceptionMessage + " " + rootCauseMessage).toLowerCase();

            // Duplicate key violations
            if (combinedMessage.contains("duplicate") || combinedMessage.contains("unique")) {
                if (combinedMessage.contains("name")) {
                    return "A record with this name already exists";
                } else if (combinedMessage.contains("email")) {
                    return "This email address is already in use";
                } else if (combinedMessage.contains("asset_tag")) {
                    return "This asset tag is already in use";
                }
                return "This record already exists. Please use unique values";
            }

            // Foreign key violations
            if (combinedMessage.contains("foreign key")) {
                return "Cannot perform this operation due to related records. Please check your data";
            }

            // Check constraint violations
            if (combinedMessage.contains("check constraint")) {
                if (combinedMessage.contains("purchase_date") || combinedMessage.contains("purchasedate")) {
                    return "Purchase date cannot be in the future";
                }
                return "The provided data violates database constraints";
            }

            // NOT NULL violations
            if (combinedMessage.contains("not null") || combinedMessage.contains("cannot be null")) {
                return "Required field is missing";
            }
        }

        // Default message for unknown database errors
        return "Database constraint violation. Please check your input data";
    }
}