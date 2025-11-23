package com.workflow.common.exception.handler;

import com.workflow.common.exception.ResponseBuilder;
import com.workflow.common.exception.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.file.AccessDeniedException;

@Component
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
}
