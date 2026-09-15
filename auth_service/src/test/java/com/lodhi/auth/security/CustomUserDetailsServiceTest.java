package com.lodhi.auth.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.lodhi.auth.enums.IdentifiersTypes;
import com.lodhi.auth.model.User;
import com.lodhi.auth.respositories.UserRepository;
import com.lodhi.auth.utils.IdentifierUtils;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private IdentifierUtils identifierUtils;

    @Mock
    private User user;

    @Test
    void loadUserByUsername_Email_Success() {
        when(identifierUtils.resolveType("test@example.com")).thenReturn(IdentifiersTypes.EMAIL);
        when(userRepository.findByEmailWithRolesAndPermissions("test@example.com")).thenReturn(Optional.of(user));

        UserDetails result = customUserDetailsService.loadUserByUsername("test@example.com");
        assertEquals(user, result);
    }

    @Test
    void loadUserByUsername_Phone_Success() {
        when(identifierUtils.resolveType("9998887776")).thenReturn(IdentifiersTypes.PHONE);
        when(userRepository.findByPhoneNumber("9998887776")).thenReturn(Optional.of(user));

        UserDetails result = customUserDetailsService.loadUserByUsername("9998887776");
        assertEquals(user, result);
    }

    @Test
    void loadUserByUsername_Username_Success() {
        when(identifierUtils.resolveType("testuser")).thenReturn(IdentifiersTypes.USERNAME);
        when(userRepository.findByUsernameWithRolesAndPermissions("testuser")).thenReturn(Optional.of(user));

        UserDetails result = customUserDetailsService.loadUserByUsername("testuser");
        assertEquals(user, result);
    }

    @Test
    void loadUserByUsername_UserNotFound_ThrowsException() {
        when(identifierUtils.resolveType("notfound@example.com")).thenReturn(IdentifiersTypes.EMAIL);
        when(userRepository.findByEmailWithRolesAndPermissions("notfound@example.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> {
            customUserDetailsService.loadUserByUsername("notfound@example.com");
        });
    }
}
