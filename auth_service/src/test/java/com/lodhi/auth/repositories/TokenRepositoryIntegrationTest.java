package com.lodhi.auth.repositories;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.lodhi.auth.BaseIntegrationTest;
import com.lodhi.auth.enums.Gender;
import com.lodhi.auth.enums.Provider;
import com.lodhi.auth.model.RefreshToken;
import com.lodhi.auth.model.User;
import com.lodhi.auth.respositories.RefreshTokenRepository;
import com.lodhi.auth.respositories.UserRepository;

@org.springframework.transaction.annotation.Transactional
class TokenRepositoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    private User savedUser;
    private RefreshToken savedToken;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setUsername("tokenuser");
        user.setEmail("tokenuser@example.com");
        user.setPassword("password");
        user.setName("Token User");
        user.setGender(Gender.MALE);
        user.setPhoneNumber("1231231234");
        user.setEnabled(true);
        user.setProvider(Provider.LOCAL);
        savedUser = userRepository.save(user);

        RefreshToken token = RefreshToken.builder()
                .jti("jti-123")
                .jtiHash("hashed-jti-123")
                .familyId("family-123")
                .user(savedUser)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();
        savedToken = refreshTokenRepository.save(token);
    }

    @Test
    void testFindByJtiHash() {
        Optional<RefreshToken> found = refreshTokenRepository.findByJtiHash("hashed-jti-123");
        assertTrue(found.isPresent());
        assertEquals("jti-123", found.get().getJti());
    }

    @Test
    void testRevokeIfActive() {
        int updatedCount = refreshTokenRepository.revokeIfActive("hashed-jti-123", "new-jti-456", Instant.now().minusSeconds(10));
        assertEquals(1, updatedCount);

        Optional<RefreshToken> found = refreshTokenRepository.findByJtiHash("hashed-jti-123");
        assertTrue(found.isPresent());
        assertTrue(found.get().isRevoked());
        assertEquals("new-jti-456", found.get().getReplaceToken());
    }

    @Test
    void testRevokeFamily() {
        // Add a second token to the family
        RefreshToken token2 = RefreshToken.builder()
                .jti("jti-456")
                .jtiHash("hashed-jti-456")
                .familyId("family-123")
                .user(savedUser)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();
        refreshTokenRepository.save(token2);

        int updatedCount = refreshTokenRepository.revokeFamily("family-123");
        assertEquals(2, updatedCount);

        Optional<RefreshToken> t1 = refreshTokenRepository.findByJtiHash("hashed-jti-123");
        Optional<RefreshToken> t2 = refreshTokenRepository.findByJtiHash("hashed-jti-456");

        assertTrue(t1.get().isRevoked());
        assertTrue(t2.get().isRevoked());
    }

    @Test
    void testRevokeAllForUser() {
        int updatedCount = refreshTokenRepository.revokeAllForUser(savedUser.getId());
        assertEquals(1, updatedCount);

        Optional<RefreshToken> t1 = refreshTokenRepository.findByJtiHash("hashed-jti-123");
        assertTrue(t1.get().isRevoked());
    }
}
