package com.workflow.common.exception;

import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global Exception Handler Coordinator
 *
 * This class serves as the main RestControllerAdvice coordinator for exception handling.
 * Specific exception handlers are delegated to modular handler components:
 * - ValidationExceptionHandler: Handles validation and request parsing errors
 * - BusinessExceptionHandler: Handles business logic exceptions
 * - AuthenticationExceptionHandler: Handles authentication-related exceptions
 * - JwtExceptionHandler: Handles JWT token validation exceptions
 * - SecurityExceptionHandler: Handles authorization and security exceptions
 * - GeneralExceptionHandler: Handles unexpected exceptions
 *
 * Benefits of this modular approach:
 * - Single Responsibility: Each handler manages its own exception category
 * - Easier Maintenance: Add new handlers without modifying this file
 * - Better Testing: Test each handler independently
 * - Cleaner Code: Reduced file size and improved readability
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    // All exception handling is delegated to specific handler components
    // See the handler/ package for implementation details
}

