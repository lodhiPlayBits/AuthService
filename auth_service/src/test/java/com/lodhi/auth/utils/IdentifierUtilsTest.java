package com.lodhi.auth.utils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.lodhi.auth.enums.IdentifiersTypes;
import com.lodhi.auth.exceptions.InvalidIdentifierException;
import com.lodhi.auth.utils.validation.IdentifierMatcher;

class IdentifierUtilsTest {

    private IdentifierMatcher emailMatcher;
    private IdentifierMatcher phoneMatcher;
    private IdentifierUtils identifierUtils;

    @BeforeEach
    void setUp() {
        emailMatcher = mock(IdentifierMatcher.class);
        phoneMatcher = mock(IdentifierMatcher.class);
        
        when(emailMatcher.getType()).thenReturn(IdentifiersTypes.EMAIL);
        when(phoneMatcher.getType()).thenReturn(IdentifiersTypes.PHONE);

        identifierUtils = new IdentifierUtils(Arrays.asList(emailMatcher, phoneMatcher));
    }

    @Test
    void testResolveType_Email() {
        String identifier = "test@example.com";
        when(emailMatcher.matches(identifier)).thenReturn(true);
        
        IdentifiersTypes type = identifierUtils.resolveType(identifier);
        assertEquals(IdentifiersTypes.EMAIL, type);
    }

    @Test
    void testResolveType_Phone() {
        String identifier = "1234567890";
        when(emailMatcher.matches(identifier)).thenReturn(false);
        when(phoneMatcher.matches(identifier)).thenReturn(true);
        
        IdentifiersTypes type = identifierUtils.resolveType(identifier);
        assertEquals(IdentifiersTypes.PHONE, type);
    }

    @Test
    void testResolveType_Blank() {
        assertThrows(InvalidIdentifierException.class, () -> identifierUtils.resolveType("   "));
        assertThrows(InvalidIdentifierException.class, () -> identifierUtils.resolveType(null));
    }

    @Test
    void testResolveType_Unresolved() {
        String identifier = "invalid";
        when(emailMatcher.matches(identifier)).thenReturn(false);
        when(phoneMatcher.matches(identifier)).thenReturn(false);
        
        assertThrows(InvalidIdentifierException.class, () -> identifierUtils.resolveType(identifier));
    }
}
