package com.lodhi.auth.respositories;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lodhi.auth.model.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken,UUID> {
    
    /**
     * Find refresh token by JTI (legacy - prefer findByJtiHash)
     * @deprecated Use findByJtiHash for secure lookup
     */
    @Deprecated
    Optional<RefreshToken> findByJti(String jti);
    
    /**
     * Find refresh token by hashed JTI (secure lookup method)
     */
    Optional<RefreshToken> findByJtiHash(String jtiHash);
    
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
       UPDATE RefreshToken r SET r.revoked = true, r.replaceToken = :newJti WHERE r.jti = :jti AND r.revoked = false AND r.expiresAt > :now""")
    int revokeIfActive(@Param("jti") String jti, @Param("newJti") String newJti, @Param("now") Instant now);


    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.familyId = :familyId AND r.revoked = false")
    int revokeFamily(@Param("familyId") String familyId);

    /**
     * Revoke all refresh tokens for a user (logout from all devices)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.user.id = :userId AND r.revoked = false")
    int revokeAllForUser(@Param("userId") Long userId);


}
