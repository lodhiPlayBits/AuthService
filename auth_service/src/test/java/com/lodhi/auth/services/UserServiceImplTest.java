package com.lodhi.auth.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.lodhi.auth.constants.SystemRoles;
import com.lodhi.auth.dtos.ChangePasswordRequestDTO;
import com.lodhi.auth.dtos.UpdateUserRequestDTO;
import com.lodhi.auth.dtos.request.CreateUserRequestDTO;
import com.lodhi.auth.dtos.response.CreateUserResponseDTO;
import com.lodhi.auth.exceptions.registration.UserAlreadyExistsException;
import com.lodhi.auth.exceptions.resource.ResourceNotFoundException;
import com.lodhi.auth.exceptions.validation.ValidationException;
import com.lodhi.auth.model.Role;
import com.lodhi.auth.model.User;
import com.lodhi.auth.respositories.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private ModelMapper modelMapper;
    @Mock private RoleService roleService;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("encoded_password")
                .build();
    }

    @Test
    void testCreateUser_Success() {
        CreateUserRequestDTO req = new CreateUserRequestDTO();
        req.setEmail("test@example.com");
        req.setUsername("testuser");
        req.setPhoneNumber("1234567890");
        req.setPassword("raw_password");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(anyString())).thenReturn(false);

        User mappedUser = new User();
        when(modelMapper.map(req, User.class)).thenReturn(mappedUser);
        when(passwordEncoder.encode("raw_password")).thenReturn("encoded_password");

        Role userRole = new Role();
        when(roleService.getRoleByName(SystemRoles.USER)).thenReturn(userRole);
        when(userRepository.save(any(User.class))).thenReturn(user);

        CreateUserResponseDTO expectedResponse = new CreateUserResponseDTO();
        when(modelMapper.map(user, CreateUserResponseDTO.class)).thenReturn(expectedResponse);

        CreateUserResponseDTO result = userService.createUser(req);

        assertEquals(expectedResponse, result);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testCreateUser_EmailAlreadyExists() {
        CreateUserRequestDTO req = new CreateUserRequestDTO();
        req.setEmail("test@example.com");
        req.setUsername("testuser");
        req.setPhoneNumber("1234567890");

        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> userService.createUser(req));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testCreateUser_UsernameAlreadyExists() {
        CreateUserRequestDTO req = new CreateUserRequestDTO();
        req.setEmail("test@example.com");
        req.setUsername("testuser");
        req.setPhoneNumber("1234567890");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> userService.createUser(req));
    }

    @Test
    void testCreateUser_PhoneNumberAlreadyExists() {
        CreateUserRequestDTO req = new CreateUserRequestDTO();
        req.setEmail("test@example.com");
        req.setUsername("testuser");
        req.setPhoneNumber("1234567890");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(anyString())).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> userService.createUser(req));
    }

    @Test
    void testCreateUser_DataIntegrityViolation_Email() {
        CreateUserRequestDTO req = new CreateUserRequestDTO();
        req.setEmail("test@example.com");
        req.setUsername("testuser");
        req.setPhoneNumber("1234567890");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(anyString())).thenReturn(false);
        when(modelMapper.map(req, User.class)).thenReturn(new User());
        when(roleService.getRoleByName(SystemRoles.USER)).thenReturn(new Role());

        when(userRepository.save(any(User.class)))
            .thenThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate key value violates unique constraint \"user_table_email_key\""));

        assertThrows(UserAlreadyExistsException.class, () -> userService.createUser(req));
    }

    @Test
    void testCreateUser_DataIntegrityViolation_Username() {
        CreateUserRequestDTO req = new CreateUserRequestDTO();
        req.setEmail("test@example.com");
        req.setUsername("testuser");
        req.setPhoneNumber("1234567890");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(anyString())).thenReturn(false);
        when(modelMapper.map(req, User.class)).thenReturn(new User());
        when(roleService.getRoleByName(SystemRoles.USER)).thenReturn(new Role());

        when(userRepository.save(any(User.class)))
            .thenThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate key value violates unique constraint \"user_table_username_key\""));

        assertThrows(UserAlreadyExistsException.class, () -> userService.createUser(req));
    }

    @Test
    void testCreateUser_DataIntegrityViolation_Phone() {
        CreateUserRequestDTO req = new CreateUserRequestDTO();
        req.setEmail("test@example.com");
        req.setUsername("testuser");
        req.setPhoneNumber("1234567890");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(anyString())).thenReturn(false);
        when(modelMapper.map(req, User.class)).thenReturn(new User());
        when(roleService.getRoleByName(SystemRoles.USER)).thenReturn(new Role());

        when(userRepository.save(any(User.class)))
            .thenThrow(new org.springframework.dao.DataIntegrityViolationException("user_table_phone_number_key"));

        assertThrows(UserAlreadyExistsException.class, () -> userService.createUser(req));
    }

    @Test
    void testCreateUser_DataIntegrityViolation_Generic() {
        CreateUserRequestDTO req = new CreateUserRequestDTO();
        req.setEmail("test@example.com");
        req.setUsername("testuser");
        req.setPhoneNumber("1234567890");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(anyString())).thenReturn(false);
        when(modelMapper.map(req, User.class)).thenReturn(new User());
        when(roleService.getRoleByName(SystemRoles.USER)).thenReturn(new Role());

        when(userRepository.save(any(User.class)))
            .thenThrow(new org.springframework.dao.DataIntegrityViolationException("some other constraint"));

        assertThrows(UserAlreadyExistsException.class, () -> userService.createUser(req));
    }

    @Test
    void testGetUserById_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        
        CreateUserResponseDTO expected = new CreateUserResponseDTO();
        when(modelMapper.map(user, CreateUserResponseDTO.class)).thenReturn(expected);
        
        CreateUserResponseDTO result = userService.getUserById(1L);
        assertEquals(expected, result);
    }

    @Test
    void testGetUserById_NotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(99L));
    }

    @Test
    void testUpdateUser_Success() {
        UpdateUserRequestDTO req = new UpdateUserRequestDTO();
        req.setName("New Name");
        req.setGender(com.lodhi.auth.enums.Gender.FEMALE);
        req.setImage("new_image.png");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        
        CreateUserResponseDTO expected = new CreateUserResponseDTO();
        when(modelMapper.map(user, CreateUserResponseDTO.class)).thenReturn(expected);
        
        CreateUserResponseDTO result = userService.updateUser(req, 1L);
        
        assertEquals(expected, result);
        assertEquals("New Name", user.getName());
        assertEquals(com.lodhi.auth.enums.Gender.FEMALE, user.getGender());
        assertEquals("new_image.png", user.getImage());
    }

    @Test
    void testUpdateUser_NullFields_ShouldNotUpdate() {
        UpdateUserRequestDTO req = new UpdateUserRequestDTO();
        // all fields null
        
        user.setName("Old Name");
        user.setGender(com.lodhi.auth.enums.Gender.MALE);
        user.setImage("old.png");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        
        userService.updateUser(req, 1L);
        
        // Assert old values remain
        assertEquals("Old Name", user.getName());
        assertEquals(com.lodhi.auth.enums.Gender.MALE, user.getGender());
        assertEquals("old.png", user.getImage());
    }

    @Test
    void testDeleteUser_Success() {
        when(userRepository.existsById(1L)).thenReturn(true);
        userService.deleteUser(1L);
        verify(userRepository).deleteById(1L);
    }

    @Test
    void testDeleteUser_NotFound() {
        when(userRepository.existsById(1L)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> userService.deleteUser(1L));
    }

    @Test
    void testChangePassword_Success() {
        ChangePasswordRequestDTO req = new ChangePasswordRequestDTO();
        req.setCurrentPassword("encoded_password");
        req.setNewPassword("new_password");
        req.setConfirmPassword("new_password");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("encoded_password", user.getPassword())).thenReturn(true);
        when(passwordEncoder.matches("new_password", user.getPassword())).thenReturn(false);
        when(passwordEncoder.encode("new_password")).thenReturn("new_encoded");

        userService.changePassword(1L, req);

        verify(userRepository).save(user);
        assertEquals("new_encoded", user.getPassword());
    }

    @Test
    void testChangePassword_MismatchNewPassword() {
        ChangePasswordRequestDTO req = new ChangePasswordRequestDTO();
        req.setCurrentPassword("encoded_password");
        req.setNewPassword("new_password");
        req.setConfirmPassword("different_password");

        assertThrows(ValidationException.class, () -> userService.changePassword(1L, req));
    }

    @Test
    void testChangePassword_WrongCurrentPassword() {
        ChangePasswordRequestDTO req = new ChangePasswordRequestDTO();
        req.setCurrentPassword("wrong_current");
        req.setNewPassword("new_password");
        req.setConfirmPassword("new_password");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong_current", user.getPassword())).thenReturn(false);

        assertThrows(ValidationException.class, () -> userService.changePassword(1L, req));
    }

    @Test
    void testChangePassword_SameAsCurrentPassword() {
        ChangePasswordRequestDTO req = new ChangePasswordRequestDTO();
        req.setCurrentPassword("encoded_password");
        req.setNewPassword("encoded_password");
        req.setConfirmPassword("encoded_password");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("encoded_password", user.getPassword())).thenReturn(true);

        assertThrows(ValidationException.class, () -> userService.changePassword(1L, req));
    }

    @Test
    void testGetAllUsers_CappedPagination() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 500);
        
        org.springframework.data.domain.Page<User> mockPage = new org.springframework.data.domain.PageImpl<>(java.util.List.of(user));
        
        // Should cap at 100
        when(userRepository.findAll(any(org.springframework.data.domain.Pageable.class))).thenAnswer(invocation -> {
            org.springframework.data.domain.Pageable p = invocation.getArgument(0);
            assertEquals(100, p.getPageSize());
            return mockPage;
        });

        org.springframework.data.domain.Page<CreateUserResponseDTO> result = userService.getAllUsers(pageable);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void testAssignRolesToUser() {
        java.util.UUID roleId = java.util.UUID.randomUUID();
        Role mockRole = new Role();
        mockRole.setName("ADMIN");
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roleService.getRoleById(roleId)).thenReturn(mockRole);
        when(userRepository.save(user)).thenReturn(user);

        // Mock SecurityContext to avoid NullPointerException in audit log
        org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("admin_user");
        org.springframework.security.core.context.SecurityContext ctx = mock(org.springframework.security.core.context.SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        org.springframework.security.core.context.SecurityContextHolder.setContext(ctx);

        userService.assignRolesToUser(1L, java.util.Set.of(roleId));

        verify(userRepository).save(user);
        assertTrue(user.getRoles().contains(mockRole));
        
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }
}
