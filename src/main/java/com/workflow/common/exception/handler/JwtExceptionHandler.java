package com.workflow.common.exception.handler;

import com.workflow.common.exception.ResponseBuilder;
import com.workflow.common.exception.ErrorResponse;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class JwtExceptionHandler {

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
}
