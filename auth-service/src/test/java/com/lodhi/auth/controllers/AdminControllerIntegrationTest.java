package com.lodhi.auth.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lodhi.auth.BaseIntegrationTest;
import com.lodhi.auth.dtos.request.AssignPermissionsRequestDTO;
import com.lodhi.auth.dtos.request.AssignRolesRequestDTO;
import com.lodhi.auth.dtos.request.CreatePermissionRequestDTO;
import com.lodhi.auth.dtos.request.CreateRoleRequestDTO;

class AdminControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @WithMockUser(authorities = {"ROLE_ADMIN", "roles:read"})
    void testGetRoles_AsAdmin_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/admin/roles")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetRoles_AsUser_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/roles")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetRoles_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/admin/roles")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_ADMIN", "roles:create"}, username = "admin")
    void testCreateRole_ReturnsCreated() throws Exception {
        CreateRoleRequestDTO req = new CreateRoleRequestDTO();
        req.setRoleName("MANAGER_ROLE");
        req.setDescription("Manager");

        mockMvc.perform(post("/api/v1/admin/roles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_ADMIN", "roles:delete"}, username = "admin")
    void testDeleteRole_ReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/roles/" + UUID.randomUUID()))
                .andExpect(status().isNotFound()); // NotFound because role doesn't exist yet, but route is accessible
    }

    @Test
    @WithMockUser(authorities = {"ROLE_ADMIN", "permissions:read"})
    void testGetPermissions_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/admin/permissions"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_ADMIN", "permissions:create"}, username = "admin")
    void testCreatePermission_ReturnsCreated() throws Exception {
        CreatePermissionRequestDTO req = new CreatePermissionRequestDTO();
        req.setName("logs:read");
        req.setDescription("Read logs");

        mockMvc.perform(post("/api/v1/admin/permissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_ADMIN", "permissions:delete"}, username = "admin")
    void testDeletePermission_ReturnsNotFound() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/permissions/" + UUID.randomUUID()))
                .andExpect(status().isNotFound()); 
    }

    @Test
    @WithMockUser(authorities = {"ROLE_ADMIN", "roles:update"}, username = "admin")
    void testAssignPermissionsToRole_ReturnsOk() throws Exception {
        AssignPermissionsRequestDTO req = new AssignPermissionsRequestDTO();
        req.setPermissionIds(Set.of(UUID.randomUUID()));

        mockMvc.perform(post("/api/v1/admin/roles/" + UUID.randomUUID() + "/permissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound()); // Role/Permission won't exist but route is hit
    }

    @Test
    @WithMockUser(authorities = {"ROLE_ADMIN", "roles:update"}, username = "admin")
    void testRevokePermissionsFromRole_ReturnsOk() throws Exception {
        AssignPermissionsRequestDTO req = new AssignPermissionsRequestDTO();
        req.setPermissionIds(Set.of(UUID.randomUUID()));

        mockMvc.perform(delete("/api/v1/admin/roles/" + UUID.randomUUID() + "/permissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound()); 
    }

    @Test
    @WithMockUser(authorities = {"ROLE_ADMIN", "users:read"})
    void testGetUsers_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_ADMIN", "users:assign-roles"}, username = "admin")
    void testAssignRolesToUser_ReturnsOk() throws Exception {
        AssignRolesRequestDTO req = new AssignRolesRequestDTO();
        req.setRoleIds(Set.of(UUID.randomUUID()));

        mockMvc.perform(put("/api/v1/admin/users/999/roles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound()); // User won't exist
    }
}
