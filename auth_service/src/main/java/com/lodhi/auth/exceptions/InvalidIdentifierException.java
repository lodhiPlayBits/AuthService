package com.lodhi.auth.exceptions;

public class InvalidIdentifierException extends RuntimeException {
    public InvalidIdentifierException(String message) {
        super(message);
    }
    public InvalidIdentifierException() {
        super("Invalid identifier");
    }
}
