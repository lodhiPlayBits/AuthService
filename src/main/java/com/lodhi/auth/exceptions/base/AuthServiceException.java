package com.lodhi.auth.exceptions.base;


import lombok.Getter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

@Getter
public abstract class AuthServiceException extends RuntimeException {
    private final HttpStatus status;
    private final ErrorCode errorCode;
    protected AuthServiceException(String message, HttpStatus status, ErrorCode errorCode) {
         super(message);
         this.status = status;
         this.errorCode = errorCode;
    }


}
