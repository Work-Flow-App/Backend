package com.workflow.common.exception.handler;

import com.workflow.common.exception.ResponseBuilder;
import com.workflow.common.exception.ErrorResponse;
import com.workflow.common.exception.customException.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class BusinessExceptionHandler {

    @ExceptionHandler(CompanyNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCompanyNotFound(
            CompanyNotFoundException ex,
            HttpServletRequest request) {
        return ResponseBuilder.buildNotFoundResponse(ex.getMessage(), request);
    }

    @ExceptionHandler(CompanyAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleCompanyAlreadyExists(
            CompanyAlreadyExistsException ex,
            HttpServletRequest request) {
        return ResponseBuilder.buildConflictResponse(ex.getMessage(), request);
    }

    @ExceptionHandler(ClientNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleClientNotFound(
            ClientNotFoundException ex,
            HttpServletRequest request) {
        return ResponseBuilder.buildNotFoundResponse(ex.getMessage(), request);
    }

    @ExceptionHandler(JobNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleJobNotFound(
            JobNotFoundException ex,
            HttpServletRequest request) {
        return ResponseBuilder.buildNotFoundResponse(ex.getMessage(), request);
    }

    @ExceptionHandler(TemplateNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTemplateNotFound(
            TemplateNotFoundException ex,
            HttpServletRequest request) {
        return ResponseBuilder.buildNotFoundResponse(ex.getMessage(), request);
    }

    @ExceptionHandler(WorkerNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleWorkerNotFound(
            WorkerNotFoundException ex,
            HttpServletRequest request) {
        return ResponseBuilder.buildNotFoundResponse(ex.getMessage(), request);
    }

    @ExceptionHandler(WorkerAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleWorkerAlreadyExists(
            WorkerAlreadyExistsException ex,
            HttpServletRequest request) {
        return ResponseBuilder.buildConflictResponse(ex.getMessage(), request);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExists(
            UserAlreadyExistsException ex,
            HttpServletRequest request) {
        return ResponseBuilder.buildConflictResponse(ex.getMessage(), request);
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
