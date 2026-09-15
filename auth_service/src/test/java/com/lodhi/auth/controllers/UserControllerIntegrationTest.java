package com.lodhi.auth.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.lodhi.auth.BaseIntegrationTest;
import com.lodhi.auth.constants.SystemRoles;
import com.lodhi.auth.dtos.LoginRequestDTO;
import com.lodhi.auth.dtos.request.CreateUserRequestDTO;
import com.lodhi.auth.model.Role;
import com.lodhi.auth.respositories.RoleRepository;
import com.lodhi.auth.respositories.UserRepository;
import com.lodhi.auth.respositories.RefreshTokenRepository;

@org.springframework.transaction.annotation.Transactional
class UserControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.lodhi.auth.respositories.PermissionRepository permissionRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String accessToken;
    private Long userId;

    @BeforeEach
    void setupRolesAndUser() throws Exception {
        refreshTokenRepository.deleteAll(); // Clean up refresh tokens to avoid FK constraint violations
        userRepository.deleteAll(); // Clean up from other tests

        Role userRole = roleRepository.findByName(SystemRoles.USER).orElseGet(() -> {
            Role role = new Role();
            role.setName(SystemRoles.USER);
            role.setDescription("User Role");
            role.setSystemRole(true);
            return role;
        });
        
        com.lodhi.auth.model.Permission userReadPerm = permissionRepository.findByName("user:read").orElseGet(() -> {
            com.lodhi.auth.model.Permission p = new com.lodhi.auth.model.Permission();
            p.setName("user:read");
            p.setDescription("Read own user data");
            return permissionRepository.save(p);
        });
        
        com.lodhi.auth.model.Permission userUpdatePerm = permissionRepository.findByName("user:update").orElseGet(() -> {
            com.lodhi.auth.model.Permission p = new com.lodhi.auth.model.Permission();
            p.setName("user:update");
            p.setDescription("Update own user data");
            return permissionRepository.save(p);
        });
        
        if (userRole.getPermissions() == null) {
            userRole.setPermissions(new java.util.HashSet<>());
        }
        userRole.getPermissions().add(userReadPerm);
        userRole.getPermissions().add(userUpdatePerm);
        roleRepository.save(userRole);

        // Register User
        CreateUserRequestDTO registerReq = new CreateUserRequestDTO();
        registerReq.setUsername("user_test");
        registerReq.setEmail("user_test@example.com");
        registerReq.setPassword("Password@123");
        registerReq.setPhoneNumber("9998887776");
        registerReq.setName("User Test");
        registerReq.setGender(com.lodhi.auth.enums.Gender.MALE);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        // Login User to get JWT
        LoginRequestDTO loginReq = new LoginRequestDTO("user_test@example.com", "Password@123");
        
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        String responseString = result.getResponse().getContentAsString();
        accessToken = JsonPath.read(responseString, "$.accessToken");
        
        // Extract userId for path variables
        Integer idInt = JsonPath.read(responseString, "$.user.id");
        userId = idInt.longValue();
    }

    @Test
    void testGetUserById_Success() throws Exception {
        mockMvc.perform(get("/api/v1/users/" + userId)
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isFound()) // Method returns HttpStatus.FOUND (302)
                .andExpect(jsonPath("$.email").value("user_test@example.com"));
    }

    @Test
    void testGetUserById_Unauthorized_WithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/" + userId))
                .andExpect(status().isUnauthorized()); // No token -> 401
    }

    @Test
    void testChangePassword_Success() throws Exception {
        com.lodhi.auth.dtos.ChangePasswordRequestDTO changePasswordReq = new com.lodhi.auth.dtos.ChangePasswordRequestDTO();
        changePasswordReq.setCurrentPassword("Password@123");
        changePasswordReq.setNewPassword("NewPassword@123");
        changePasswordReq.setConfirmPassword("NewPassword@123");

        mockMvc.perform(post("/api/v1/users/" + userId + "/change-password")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(changePasswordReq)))
                .andExpect(status().isOk());
                
        // Verify we can login with the new password
        LoginRequestDTO loginReq = new LoginRequestDTO("user_test@example.com", "NewPassword@123");
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk());
    }
}
