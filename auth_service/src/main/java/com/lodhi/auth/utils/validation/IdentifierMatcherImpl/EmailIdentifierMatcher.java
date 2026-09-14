package com.lodhi.auth.utils.validation.IdentifierMatcherImpl;


import com.lodhi.auth.enums.IdentifiersTypes;
import com.lodhi.auth.utils.validation.IdentifierMatcher;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.util.regex.Pattern;

@Component
@Order(1)
public class EmailIdentifierMatcher implements IdentifierMatcher {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    @Override
    public boolean matches(String identifier) {
        return EMAIL_PATTERN.matcher(identifier).matches();
    }

    @Override
    public IdentifiersTypes getType() {
        return IdentifiersTypes.EMAIL;
    }
}