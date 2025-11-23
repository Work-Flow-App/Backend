package com.workflow.common.exception;

import com.workflow.common.exception.customException.*;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.nio.file.AccessDeniedException;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global Exception Handler
 *
 * Centralized exception handling for all REST endpoints.
 * Handles all exception types including:
 * - Validation errors (MethodArgumentNotValidException, HttpMessageNotReadableException)
 * - Business logic exceptions (NotFound, AlreadyExists)
 * - Authentication errors (BadCredentials, UsernameNotFound, Token-related)
 * - JWT validation errors (ExpiredJwt, MalformedJwt, SignatureException)
 * - Security/Authorization errors (AccessDenied)
 * - General unexpected exceptions
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ==================== VALIDATION HANDLERS ====================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> validationErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        fieldError -> fieldError.getField(),
                        fieldError -> fieldError.getDefaultMessage(),
                        (existing, replacement) -> existing
                ));

        return ResponseBuilder.buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                request,
                validationErrors
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        String message = "Malformed JSON request";

        if (ex.getCause() != null && ex.getCause().getMessage() != null) {
            String causeMessage = ex.getCause().getMessage();
            if (causeMessage.contains("not one of the values accepted for Enum")) {
                message = "Invalid value provided for enum field";
            }
        }

        return ResponseBuilder.buildBadRequestResponse(message, request);
    }

    // ==================== BUSINESS LOGIC HANDLERS ====================

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

    // ==================== AUTHENTICATION HANDLERS ====================

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex,
            HttpServletRequest request) {
        return ResponseBuilder.buildUnauthorizedResponse("Invalid username or password", request);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFound(
            UsernameNotFoundException ex,
            HttpServletRequest request) {
        return ResponseBuilder.buildUnauthorizedResponse(ex.getMessage(), request);
    }

    @ExceptionHandler({LockedException.class, DisabledException.class,
                      AccountExpiredException.class, CredentialsExpiredException.class})
    public ResponseEntity<ErrorResponse> handleAccountExceptions(
            RuntimeException ex,
            HttpServletRequest request) {
        return ResponseBuilder.buildUnauthorizedResponse(ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRefreshToken(
            InvalidRefreshTokenException ex,
            HttpServletRequest request) {
        return ResponseBuilder.buildUnauthorizedResponse(ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidPasswordResetTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPasswordResetToken(
            InvalidPasswordResetTokenException ex,
            HttpServletRequest request) {
        return ResponseBuilder.buildBadRequestResponse(ex.getMessage(), request);
    }

    // ==================== JWT HANDLERS ====================

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ErrorResponse> handleExpiredJwt(
            ExpiredJwtException ex,
            HttpServletRequest request) {
        return ResponseBuilder.buildUnauthorizedResponse("JWT token expired", request);
    }

    @ExceptionHandler(SignatureException.class)
    public ResponseEntity<ErrorResponse> handleInvalidSignature(
            SignatureException ex,
            HttpServletRequest request) {
        return ResponseBuilder.buildForbiddenResponse("JWT signature does not match", request);
    }

    @ExceptionHandler({MalformedJwtException.class, UnsupportedJwtException.class, JwtException.class})
    public ResponseEntity<ErrorResponse> handleInvalidJwt(
            RuntimeException ex,
            HttpServletRequest request) {
        return ResponseBuilder.buildUnauthorizedResponse("Invalid JWT token", request);
    }

    // ==================== SECURITY/AUTHORIZATION HANDLERS ====================

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {
        return ResponseBuilder.buildForbiddenResponse(
                "You do not have permission to access this resource",
                request
        );
    }

    // ==================== GENERAL FALLBACK HANDLER ====================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(
            Exception ex,
            HttpServletRequest request) {
        return ResponseBuilder.buildInternalServerErrorResponse(ex.getMessage(), request);
    }
}

