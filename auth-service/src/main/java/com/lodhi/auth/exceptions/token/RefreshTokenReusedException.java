package com.lodhi.auth.exceptions.token;

import com.lodhi.auth.exceptions.base.AuthServiceException;
import com.lodhi.auth.exceptions.base.ErrorCode;
import org.springframework.http.HttpStatus;

public class RefreshTokenReusedException extends AuthServiceException {
    // Thrower's responsibility: revoke ALL sessions for this user when this fires — reused
    // refresh token = probable token theft. This exception should never fire silently.
    public RefreshTokenReusedException() {
        super("Refresh token reuse detected, all sessions revoked", HttpStatus.UNAUTHORIZED, ErrorCode.REFRESH_TOKEN_REUSED);
    }
}