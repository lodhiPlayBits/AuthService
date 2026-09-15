package com.lodhi.auth.exceptions.handler;

import com.lodhi.auth.exceptions.errorDTO.ErrorMessage;
import com.lodhi.auth.exceptions.base.AuthServiceException;
import com.lodhi.auth.exceptions.base.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Handles ALL custom exceptions polymorphically — one method, not one per exception class.
    @ExceptionHandler(AuthServiceException.class)
    public ResponseEntity<ErrorMessage> handleAuthServiceException(AuthServiceException ex) {
        log.warn("AuthServiceException [{}]: {}", ex.getErrorCode(), ex.getMessage());
        ErrorMessage body = ErrorMessage.of(ex.getMessage(), ex.getErrorCode(), ex.getStatus());
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    // Handle validation errors from @Valid annotation
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Validation failed");
        response.put("errorCode", ErrorCode.VALIDATION_ERROR);
        response.put("statusCode", HttpStatus.BAD_REQUEST.value());
        response.put("status", HttpStatus.BAD_REQUEST);
        response.put("errors", errors);
        response.put("correlationId", com.lodhi.auth.utils.CorrelationIdUtils.getCurrentCorrelationId());
        
        log.warn("Validation failed: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // Spring Security's own hierarchy — covers BadCredentialsException, UsernameNotFoundException, etc.
    // Generic message on purpose: prevents username enumeration.
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorMessage> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getClass().getSimpleName());
        ErrorMessage body = ErrorMessage.of("Invalid username or password", ErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorMessage> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        ErrorMessage body = ErrorMessage.of("You do not have permission to perform this action",
                ErrorCode.INSUFFICIENT_PERMISSION, HttpStatus.FORBIDDEN);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    // Catch-all — real cause logged server-side, generic message returned to client.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorMessage> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        ErrorMessage body = ErrorMessage.of("An unexpected error occurred",
                ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}