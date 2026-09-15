package com.lodhi.auth.repositories;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.lodhi.auth.BaseIntegrationTest;
import com.lodhi.auth.enums.Provider;
import com.lodhi.auth.model.Role;
import com.lodhi.auth.model.User;
import com.lodhi.auth.respositories.RoleRepository;
import com.lodhi.auth.respositories.UserRepository;

class UserRepositoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void testSaveAndFindUser() {
        // Create Role
        Role userRole = new Role();
        userRole.setName("TEST_USER_ROLE");
        userRole.setDescription("Test Role");
        roleRepository.save(userRole);

        // Create User
        User user = new User();
        user.setEmail("repo_test@example.com");
        user.setUsername("repotest");
        user.setPhoneNumber("7788990011");
        user.setPassword("encoded");
        user.setName("Test User");
        user.setGender(com.lodhi.auth.enums.Gender.MALE);
        user.setProvider(Provider.LOCAL);
        user.setEnabled(true);
        user.setRoles(Set.of(userRole));

        User savedUser = userRepository.save(user);
        assertNotNull(savedUser.getId());

        // Find User
        Optional<User> foundUser = userRepository.findByEmail("repo_test@example.com");
        assertTrue(foundUser.isPresent());
        assertEquals("repo_test@example.com", foundUser.get().getUsername());
        
        Optional<User> foundUserWithRoles = userRepository.findByIdWithRolesAndPermissions(savedUser.getId());
        assertTrue(foundUserWithRoles.isPresent());
        assertFalse(foundUserWithRoles.get().getRoles().isEmpty());
    }

    @Test
    void testExistsMethods() {
        User user = new User();
        user.setEmail("existstest@example.com");
        user.setUsername("existstest");
        user.setPhoneNumber("5544332211");
        user.setPassword("encoded");
        user.setName("Test User");
        user.setGender(Provider.LOCAL == null ? null : com.lodhi.auth.enums.Gender.MALE); // Dummy gender
        user.setProvider(Provider.LOCAL);
        user.setEnabled(true);
        userRepository.save(user);

        assertTrue(userRepository.existsByEmail("existstest@example.com"));
        assertTrue(userRepository.existsByUsername("existstest"));
        assertTrue(userRepository.existsByPhoneNumber("5544332211"));
        
        assertFalse(userRepository.existsByEmail("nonexistent@example.com"));
    }
}
