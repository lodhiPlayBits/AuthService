package com.lodhi.auth.respositories;

import com.lodhi.auth.model.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken,UUID> {
    Optional<RefreshToken> findByJti(String jti);
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
       UPDATE RefreshToken r SET r.revoked = true, r.replaceToken = :newJti WHERE r.jti = :jti AND r.revoked = false AND r.expiresAt > :now""")
    int revokeIfActive(@Param("jti") String jti, @Param("newJti") String newJti, @Param("now") Instant now);


    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.familyId = :familyId AND r.revoked = false")
    int revokeFamily(@Param("familyId") String familyId);


}
