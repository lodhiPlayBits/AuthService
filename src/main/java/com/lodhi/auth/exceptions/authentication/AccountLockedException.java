package com.lodhi.auth.exceptions.authentication;


import com.lodhi.auth.exceptions.base.AuthServiceException;
import com.lodhi.auth.exceptions.base.ErrorCode;
import org.springframework.http.HttpStatus;

public class AccountLockedException extends AuthServiceException {
    // Message intentionally generic — do NOT expose lockout details externally (enumeration risk).
    // Log the real reason server-side where this is thrown, not in the message shown to the client.
    public AccountLockedException() {
        super("Invalid username or password", HttpStatus.UNAUTHORIZED, ErrorCode.ACCOUNT_LOCKED);
    }
}