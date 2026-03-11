package com.workflow.common.exception.handler;

import com.workflow.common.exception.ResponseBuilder;
import com.workflow.common.exception.ErrorResponse;
import com.workflow.common.exception.business.InvalidGoogleTokenException;
import com.workflow.common.exception.business.InvalidPasswordResetTokenException;
import com.workflow.common.exception.business.InvalidRefreshTokenException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthenticationExceptionHandler {

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

    @ExceptionHandler(InvalidGoogleTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidGoogleToken(
            InvalidGoogleTokenException ex,
            HttpServletRequest request) {
        return ResponseBuilder.buildUnauthorizedResponse(ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidPasswordResetTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPasswordResetToken(
            InvalidPasswordResetTokenException ex,
            HttpServletRequest request) {
        return ResponseBuilder.buildBadRequestResponse(ex.getMessage(), request);
    }
}
