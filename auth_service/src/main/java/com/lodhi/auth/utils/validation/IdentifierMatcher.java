package com.lodhi.auth.utils.validation;

import com.lodhi.auth.enums.IdentifiersTypes;

public interface IdentifierMatcher {
    boolean matches(String identifier);
    IdentifiersTypes getType();
}