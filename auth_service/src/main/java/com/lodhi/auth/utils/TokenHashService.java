package com.lodhi.auth.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.springframework.stereotype.Service;

/**
 * Service for hashing JTI values for secure database storage.
 * Uses SHA-256 to create one-way hash of JTI, preventing token forgery
 * even if database is compromised.
 */
@Service
public class TokenHashService {

    /**
     * Hash a JTI using SHA-256.
     * 
     * @param jti The JWT ID to hash
     * @return Hex-encoded SHA-256 hash of the JTI
     * @throws RuntimeException if SHA-256 algorithm is not available
     */
    public String hashJti(String jti) {
        if (jti == null || jti.isBlank()) {
            throw new IllegalArgumentException("JTI cannot be null or blank");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(jti.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 should always be available in modern JVMs
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Convert byte array to hex string.
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
