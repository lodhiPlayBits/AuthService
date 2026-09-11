package com.lodhi.auth.exceptions;

import com.lodhi.auth.dtos.ErrorMessage;
import io.jsonwebtoken.JwtException;
import org.apache.logging.log4j.CloseableThreadContext;
import org.springframework.boot.web.error.Error;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.Timestamp;
import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorMessage>handleResourceNotFountException(ResourceNotFoundException exception){
        ErrorMessage errorMessage= new ErrorMessage(exception.getMessage(),HttpStatus.NOT_FOUND,"not found", Instant.now());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorMessage);

    }

    @ExceptionHandler(BlankFieldException.class)
    public ResponseEntity<ErrorMessage>handleBlankFieldException(BlankFieldException exception){
        ErrorMessage errorMessage= new ErrorMessage(exception.getMessage(),HttpStatus.BAD_REQUEST,"Field Required", Instant.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
    }
    @ExceptionHandler(JwtExpireException.class)
    public ResponseEntity<ErrorMessage>handleJwtExpireException(JwtExpireException exception){
        ErrorMessage errorMessage= new ErrorMessage(exception.getMessage(),HttpStatus.UNAUTHORIZED,"JWT Expired", Instant.now());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorMessage);
    }

    @ExceptionHandler({
            UsernameNotFoundException.class,
            BadCredentialsException.class,


            AuthenticationException.class
    })
    public ResponseEntity<ErrorMessage>handleException(Exception exception){
        ErrorMessage errorMessage= new ErrorMessage(exception.getMessage(),HttpStatus.BAD_REQUEST,"Bad Request", Instant.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
    }


}
