package com.workflow.common.exception.handler;

import com.workflow.common.exception.ResponseBuilder;
import com.workflow.common.exception.ErrorResponse;
import com.workflow.common.exception.base.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
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

    /**
     * Handles ForbiddenException subclasses (403 Forbidden)
     * - ForbiddenActionException
     */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(
            ForbiddenException ex,
            HttpServletRequest request) {
        return ResponseBuilder.buildForbiddenResponse(ex.getMessage(), request);
    }

    /**
     * Handles IllegalStateException (409 Conflict)
     * - Used for state-machine violations (e.g., archiving an assigned asset)
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException ex,
            HttpServletRequest request) {
        return ResponseBuilder.buildConflictResponse(ex.getMessage(), request);
    }
}
