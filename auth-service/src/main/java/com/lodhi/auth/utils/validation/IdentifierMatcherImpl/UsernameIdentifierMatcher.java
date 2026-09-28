package com.lodhi.auth.utils.validation.IdentifierMatcherImpl;

import com.lodhi.auth.enums.IdentifiersTypes;
import com.lodhi.auth.utils.validation.IdentifierMatcher;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
@Order(Integer.MAX_VALUE) // always evaluated last
public class UsernameIdentifierMatcher implements IdentifierMatcher {

    // alphanumeric + underscore/dot, 3-30 chars — adjust to your actual username rules
    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._]{3,30}$");

    @Override
    public boolean matches(String identifier) {
        return USERNAME_PATTERN.matcher(identifier).matches();
    }

    @Override
    public IdentifiersTypes getType() {
        return IdentifiersTypes.USERNAME;
    }
}