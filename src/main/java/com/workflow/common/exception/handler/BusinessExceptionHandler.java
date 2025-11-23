package com.workflow.common.exception.handler;

import com.workflow.common.exception.ResponseBuilder;
import com.workflow.common.exception.ErrorResponse;
import com.workflow.common.exception.customException.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;

@Component
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
}
