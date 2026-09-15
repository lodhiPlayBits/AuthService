package com.lodhi.auth.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lodhi.auth.exceptions.base.ErrorCode;
import org.springframework.http.HttpStatus;

import java.time.Instant;

public record ErrorMessage(
        String message,
        ErrorCode errorCode,
        int statusCode,
        HttpStatus status,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant timestamp
) {
    public static ErrorMessage of(String message, ErrorCode errorCode, HttpStatus status) {
        return new ErrorMessage(message, errorCode, status.value(), status, Instant.now());
    }
}