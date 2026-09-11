package com.lodhi.auth.respositories;

import com.lodhi.auth.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken,UUID> {
    Optional<RefreshToken> findByJti(String jti);

}
