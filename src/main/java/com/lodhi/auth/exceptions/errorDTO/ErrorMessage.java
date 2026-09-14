package com.lodhi.auth.exceptions.errorDTO;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.lodhi.auth.exceptions.base.ErrorCode;
import com.lodhi.auth.utils.CorrelationIdUtils;
import org.springframework.http.HttpStatus;

import java.time.Instant;

public record ErrorMessage(
        String message,
        ErrorCode errorCode,
        int statusCode,
        HttpStatus status,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant timestamp,
        String correlationId  // For request tracing
) {
    public static ErrorMessage of(String message, ErrorCode errorCode, HttpStatus status) {
        return new ErrorMessage(
                message, 
                errorCode, 
                status.value(), 
                status, 
                Instant.now(),
                CorrelationIdUtils.getCurrentCorrelationId()  // Auto-populate from MDC
        );
    }
}