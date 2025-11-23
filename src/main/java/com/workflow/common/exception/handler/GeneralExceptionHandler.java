package com.workflow.common.exception.handler;

import com.workflow.common.exception.ResponseBuilder;
import com.workflow.common.exception.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class GeneralExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(
            Exception ex,
            HttpServletRequest request) {
        return ResponseBuilder.buildInternalServerErrorResponse(ex.getMessage(), request);
    }
}
