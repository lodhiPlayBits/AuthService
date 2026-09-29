package com.lodhi.auth.security;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.lodhi.auth.services.BCryptService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationProvider implements AuthenticationProvider {

    private final CustomUserDetailsService userDetailsService;
    private final BCryptService bcryptService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String identifier = authentication.getName();
        String password = authentication.getCredentials().toString();

        // 1. Fetch user (DB transaction starts and ends here)
        UserDetails userDetails = userDetailsService.loadUserByUsername(identifier);

        if (!userDetails.isEnabled()) {
            throw new DisabledException("User is disabled");
        }
        if (!userDetails.isAccountNonLocked()) {
            throw new LockedException("User account is locked");
        }

        // 2. Perform BCrypt matching using the bounded queue (outside DB transaction)
        boolean valid = bcryptService.matches(password, userDetails.getPassword());
        if (!valid) {
            throw new BadCredentialsException("Invalid credentials");
        }

        // 3. Return authenticated token
        return new UsernamePasswordAuthenticationToken(userDetails, password, userDetails.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
