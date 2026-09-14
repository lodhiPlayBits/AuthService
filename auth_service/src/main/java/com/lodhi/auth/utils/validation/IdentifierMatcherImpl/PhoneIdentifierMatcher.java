package com.lodhi.auth.utils.validation.IdentifierMatcherImpl;


import com.lodhi.auth.enums.IdentifiersTypes;
import com.lodhi.auth.utils.validation.IdentifierMatcher;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
@Order(2)
public class PhoneIdentifierMatcher implements IdentifierMatcher {

    // Normalize by stripping spaces/hyphens before matching — done in matches()
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^\\+?[1-9]\\d{7,14}$");

    @Override
    public boolean matches(String identifier) {
        String normalized = identifier.replaceAll("[\\s-]", "");
        return PHONE_PATTERN.matcher(normalized).matches();
    }

    @Override
    public IdentifiersTypes getType() {
        return IdentifiersTypes.PHONE;
    }
}