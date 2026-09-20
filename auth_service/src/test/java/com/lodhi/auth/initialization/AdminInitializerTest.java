package com.lodhi.auth.initialization;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.lodhi.auth.constants.SystemRoles;
import com.lodhi.auth.model.Role;
import com.lodhi.auth.model.User;
import com.lodhi.auth.respositories.RoleRepository;
import com.lodhi.auth.respositories.UserRepository;

@ExtendWith(MockitoExtension.class)
class AdminInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminInitializer adminInitializer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(adminInitializer, "adminEmail", "Admin@admin.com");
        ReflectionTestUtils.setField(adminInitializer, "adminUsername", "Admin");
        ReflectionTestUtils.setField(adminInitializer, "adminPassword", "admin");
        ReflectionTestUtils.setField(adminInitializer, "adminPhoneNumber", "8595007855");
    }

    @Test
    void run_adminExists_ShouldReturnEarly() throws Exception {
        when(userRepository.existsByEmail("Admin@admin.com")).thenReturn(true);

        adminInitializer.run();

        verify(roleRepository, never()).findByName(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void run_adminDoesNotExist_RoleFound_ShouldSaveAdmin() throws Exception {
        when(userRepository.existsByEmail("Admin@admin.com")).thenReturn(false);
        Role role = new Role();
        role.setName(SystemRoles.ADMIN);
        when(roleRepository.findByName(SystemRoles.ADMIN)).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("admin")).thenReturn("encoded_admin_pass");

        adminInitializer.run();

        org.mockito.ArgumentCaptor<User> userCaptor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("Admin@admin.com", savedUser.getEmail());
        assertEquals("encoded_admin_pass", savedUser.getPassword());
        assertEquals("8595007855", savedUser.getPhoneNumber());
        assertTrue(savedUser.isEnabled());
        assertTrue(savedUser.getRoles().contains(role));
    }

    @Test
    void run_adminDoesNotExist_RoleNotFound_ShouldThrowException() {
        when(userRepository.existsByEmail("Admin@admin.com")).thenReturn(false);
        when(roleRepository.findByName(SystemRoles.ADMIN)).thenReturn(Optional.empty());

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            adminInitializer.run();
        });

        assertEquals("Admin Role not found", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }
}
