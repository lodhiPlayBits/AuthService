package com.lodhi.auth.services;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Manages a Redis-backed JWT access token blacklist.
 *
 * <p>When a user logs out (or is force-logged-out), the access token's unique
 * identifier (JTI) is stored in Redis with a TTL equal to the token's remaining
 * lifetime. Once the JWT naturally expires, Redis evicts the key automatically,
 * keeping memory usage flat.
 *
 * <p>Key schema: {@code blacklist:token:<jti>}
 */
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);
    private static final String BLACKLIST_PREFIX = "blacklist:token:";

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Adds a JWT's JTI to the blacklist in Redis.
     *
     * <p>The key will expire automatically when the JWT itself would have expired,
     * so no manual cleanup is needed.
     *
     * @param jti                  the unique JWT ID ({@code jti} claim)
     * @param expirationTimeMillis the token's {@code exp} claim in epoch millis
     */
    public void blacklistToken(String jti, long expirationTimeMillis) {
        if (jti == null || jti.isBlank()) {
            log.warn("Attempted to blacklist null or blank JTI — skipping");
            return;
        }

        long remainingTtlMillis = expirationTimeMillis - System.currentTimeMillis();

        if (remainingTtlMillis <= 0) {
            // Token is already expired — no need to store it
            log.debug("Access token JTI={} is already expired; skipping blacklist entry", jti);
            return;
        }

        String key = BLACKLIST_PREFIX + jti;
        stringRedisTemplate.opsForValue().set(key, "revoked", remainingTtlMillis, TimeUnit.MILLISECONDS);
        log.info("Access token blacklisted: jti={}, ttlMs={}", jti, remainingTtlMillis);
    }

    /**
     * Checks whether a token's JTI is currently blacklisted.
     *
     * @param jti the JWT ID to check
     * @return {@code true} if the token has been blacklisted and not yet expired
     */
    public boolean isBlacklisted(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        String key = BLACKLIST_PREFIX + jti;
        Boolean exists = stringRedisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }
}
