package com.lodhi.auth.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lodhi.auth.BaseIntegrationTest;
import com.lodhi.auth.dtos.LoginRequestDTO;
import com.lodhi.auth.dtos.request.CreateUserRequestDTO;

import com.lodhi.auth.model.Role;
import com.lodhi.auth.respositories.RoleRepository;
import com.lodhi.auth.constants.SystemRoles;
import org.junit.jupiter.api.BeforeEach;

class AuthControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private com.lodhi.auth.respositories.UserRepository userRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setupRoles() {
        if (roleRepository.findByName(SystemRoles.USER).isEmpty()) {
            Role userRole = new Role();
            userRole.setName(SystemRoles.USER);
            userRole.setDescription("User Role");
            userRole.setSystemRole(true);
            roleRepository.save(userRole);
        }
        if (roleRepository.findByName(SystemRoles.ADMIN).isEmpty()) {
            Role adminRole = new Role();
            adminRole.setName(SystemRoles.ADMIN);
            adminRole.setDescription("Admin Role");
            adminRole.setSystemRole(true);
            roleRepository.save(adminRole);
        }
    }

    @Test
    void testRegisterAndLogin() throws Exception {
        // 1. Register User
        CreateUserRequestDTO registerReq = new CreateUserRequestDTO();
        registerReq.setEmail("integration@example.com");
        registerReq.setUsername("integrationuser");
        registerReq.setName("Test User");
        registerReq.setGender(com.lodhi.auth.enums.Gender.MALE);
        registerReq.setPhoneNumber("0987654321");
        registerReq.setPassword("Password@123");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("integration@example.com"));

        // 2. Login User
        LoginRequestDTO loginReq = new LoginRequestDTO("integration@example.com", "Password@123");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(cookie().httpOnly("refreshToken", true));
    }

    @Test
    void testLogin_BadCredentials() throws Exception {
        LoginRequestDTO loginReq = new LoginRequestDTO("nonexistent@example.com", "wrongpassword");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testRegister_DuplicateEmail() throws Exception {
        // 1. Register User
        CreateUserRequestDTO registerReq = new CreateUserRequestDTO();
        registerReq.setEmail("duplicate@example.com");
        registerReq.setUsername("dupuser");
        registerReq.setName("Dup User");
        registerReq.setGender(com.lodhi.auth.enums.Gender.MALE);
        registerReq.setPhoneNumber("1122334455");
        registerReq.setPassword("Password@123");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        // 2. Try to register again with same email but different other fields
        CreateUserRequestDTO registerReq2 = new CreateUserRequestDTO();
        registerReq2.setEmail("duplicate@example.com");
        registerReq2.setUsername("dupuser2");
        registerReq2.setName("Dup User 2");
        registerReq2.setGender(com.lodhi.auth.enums.Gender.MALE);
        registerReq2.setPhoneNumber("9988776655");
        registerReq2.setPassword("Password@123");

        // Should return 409 Conflict according to global exception handler
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerReq2)))
                .andExpect(status().isConflict());
    }

    @Test
    void testAccessProtectedEndpoint_MissingToken() throws Exception {
        // Try to access a protected endpoint without a token
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/users/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testAccessProtectedEndpoint_InvalidToken() throws Exception {
        // Try to access a protected endpoint with an invalid token
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/users/1")
                .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testLogin_DisabledUser() throws Exception {
        // Create disabled user directly in database
        com.lodhi.auth.model.User disabledUser = new com.lodhi.auth.model.User();
        disabledUser.setEmail("disabled@example.com");
        disabledUser.setUsername("disableduser");
        disabledUser.setName("Disabled User");
        disabledUser.setPhoneNumber("9999999999");
        disabledUser.setPassword(passwordEncoder.encode("Password@123"));
        disabledUser.setEnabled(false);
        disabledUser.setGender(com.lodhi.auth.enums.Gender.MALE);
        disabledUser.setProvider(com.lodhi.auth.enums.Provider.LOCAL);
        userRepository.save(disabledUser);

        LoginRequestDTO loginReq = new LoginRequestDTO("disabled@example.com", "Password@123");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                // Spring Security defaults to 401 Unauthorized for DisabledException
                .andExpect(status().isUnauthorized());
    }
}
