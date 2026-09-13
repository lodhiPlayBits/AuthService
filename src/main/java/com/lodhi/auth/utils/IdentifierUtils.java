package com.lodhi.auth.utils;

import com.lodhi.auth.dtos.LoginRequestDTO;
import com.lodhi.auth.enums.IdentifiersTypes;
import com.lodhi.auth.exceptions.InvalidIdentifierException;
import com.lodhi.auth.utils.validation.IdentifierMatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;


@Component
@RequiredArgsConstructor
public class IdentifierUtils {


    private final List<IdentifierMatcher> matchers; // Spring injects in @Order sequence

    public IdentifiersTypes resolveType(String identifier) {

        if (!StringUtils.hasText(identifier)) {
            throw new InvalidIdentifierException("Identifier must not be blank");
        }

        return matchers.stream()
                .filter(matcher -> matcher.matches(identifier))
                .findFirst()
                .map(IdentifierMatcher::getType)
                .orElseThrow(() -> new InvalidIdentifierException(
                        "Unable to resolve identifier type: " + identifier));
    }

}
