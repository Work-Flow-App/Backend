package com.workflow.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

public class ResponseBuilder {

    public static ResponseEntity<ErrorResponse> buildErrorResponse(
            HttpStatus status,
            String message,
            HttpServletRequest request) {

        ErrorResponse response = new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI()
        );
        return new ResponseEntity<>(response, status);
    }

    public static ResponseEntity<ErrorResponse> buildErrorResponse(
            HttpStatus status,
            String message,
            HttpServletRequest request,
            Map<String, String> errors) {

        ErrorResponse response = new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                errors
        );
        return new ResponseEntity<>(response, status);
    }

    public static ResponseEntity<ErrorResponse> buildNotFoundResponse(
            String message,
            HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, message, request);
    }

    public static ResponseEntity<ErrorResponse> buildBadRequestResponse(
            String message,
            HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    public static ResponseEntity<ErrorResponse> buildConflictResponse(
            String message,
            HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.CONFLICT, message, request);
    }

    public static ResponseEntity<ErrorResponse> buildUnauthorizedResponse(
            String message,
            HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, message, request);
    }

    public static ResponseEntity<ErrorResponse> buildForbiddenResponse(
            String message,
            HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, message, request);
    }

    public static ResponseEntity<ErrorResponse> buildInternalServerErrorResponse(
            String message,
            HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, message, request);
    }
}
