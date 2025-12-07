package com.workflow.common.exception.handler;

import com.workflow.common.exception.ResponseBuilder;
import com.workflow.common.exception.ErrorResponse;
import com.workflow.common.exception.base.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class BusinessExceptionHandler {

    /**
     * Handles all NotFoundException subclasses (404 Not Found)
     * - CompanyNotFoundException, ClientNotFoundException, JobNotFoundException
     * - TemplateNotFoundException, WorkerNotFoundException, AssetNotFoundException
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            NotFoundException ex,
            HttpServletRequest request) {
        return ResponseBuilder.buildNotFoundResponse(ex.getMessage(), request);
    }

    /**
     * Handles all ConflictException subclasses (409 Conflict)
     * - CompanyAlreadyExistsException, UserAlreadyExistsException
     * - WorkerAlreadyExistsException, DuplicateNameException
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(
            ConflictException ex,
            HttpServletRequest request) {
        return ResponseBuilder.buildConflictResponse(ex.getMessage(), request);
    }

    /**
     * Handles all BadRequestException subclasses (400 Bad Request)
     * - InvalidPasswordResetTokenException, InvalidRefreshTokenException
     * - AssignmentException
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(
            BadRequestException ex,
            HttpServletRequest request) {
        return ResponseBuilder.buildBadRequestResponse(ex.getMessage(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {
        String message = "A database constraint was violated";

        // Extract meaningful error message from exception
        String rootCauseMessage = ex.getRootCause() != null ? ex.getRootCause().getMessage() : ex.getMessage();

        if (rootCauseMessage != null) {
            // Check for duplicate entry errors
            if (rootCauseMessage.contains("Duplicate entry")) {
                if (rootCauseMessage.contains("'UK_users_username'") || rootCauseMessage.contains("username")) {
                    message = "Username already exists";
                } else if (rootCauseMessage.contains("'UK_users_email'") || rootCauseMessage.contains("email")) {
                    message = "Email already exists";
                } else if (rootCauseMessage.contains("'user_id'")) {
                    message = "This user account is already associated with another worker";
                } else {
                    message = "This record already exists";
                }
            }
        }

        return ResponseBuilder.buildConflictResponse(message, request);
    }
}
