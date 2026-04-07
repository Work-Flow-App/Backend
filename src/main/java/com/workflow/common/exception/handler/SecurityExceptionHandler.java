package com.workflow.common.exception.handler;

import com.workflow.common.exception.ResponseBuilder;
import com.workflow.common.exception.ErrorResponse;
import com.workflow.common.exception.base.ForbiddenException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.AccessDeniedException;

@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {
        return ResponseBuilder.buildForbiddenResponse(
                "You do not have permission to access this resource",
                request
        );
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(
            ForbiddenException ex,
            HttpServletRequest request) {
        return ResponseBuilder.buildForbiddenResponse(ex.getMessage(), request);
    }
}
