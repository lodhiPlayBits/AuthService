package com.lodhi.auth.config;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

/**
 * Configures Google ID Token verification.
 * 
 * The verifier caches Google's public keys and validates:
 * - Cryptographic signature
 * - Issuer (accounts.google.com)
 * - Audience (your client ID)
 * - Expiration
 */
@Configuration
public class GoogleOAuth2Config {

    @Value("${oauth2.google.client-id}")
    private String clientId;

    @Bean
    public GoogleIdTokenVerifier googleIdTokenVerifier() {
        return new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance()
        )
        .setAudience(Collections.singletonList(clientId))
        .build();
    }
}
