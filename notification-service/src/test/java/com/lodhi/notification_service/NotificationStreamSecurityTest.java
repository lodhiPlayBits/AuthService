package com.lodhi.notification_service;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationStreamSecurityTest {

    private static final String STREAM_URL = "/api/v1/notifications/stream";

    @Autowired
    private MockMvc mockMvc;

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    @Value("${security.jwt.issuer}")
    private String jwtIssuer;

    @Value("${security.jwt.audience}")
    private String jwtAudience;

    @Test
    void streamWithoutTokenIsRejected() throws Exception {
        mockMvc.perform(get(STREAM_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void streamWithUserIdQueryParamButNoTokenIsRejected() throws Exception {
        mockMvc.perform(get(STREAM_URL).param("userId", "42"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void streamWithMalformedTokenIsRejected() throws Exception {
        mockMvc.perform(get(STREAM_URL).param("token", "not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void streamWithExpiredTokenIsRejected() throws Exception {
        String token = mintToken("42", Instant.now().minusSeconds(600), Instant.now().minusSeconds(300),
                jwtIssuer, jwtAudience);
        mockMvc.perform(get(STREAM_URL).param("token", token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void streamWithWrongIssuerIsRejected() throws Exception {
        String token = mintToken("42", Instant.now(), Instant.now().plusSeconds(300),
                "some-other-issuer", jwtAudience);
        mockMvc.perform(get(STREAM_URL).param("token", token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void streamWithWrongAudienceIsRejected() throws Exception {
        String token = mintToken("42", Instant.now(), Instant.now().plusSeconds(300),
                jwtIssuer, "some-other-audience");
        mockMvc.perform(get(STREAM_URL).param("token", token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void queryTokenIsIgnoredOutsideStreamPath() throws Exception {
        String token = mintToken("42", Instant.now(), Instant.now().plusSeconds(300),
                jwtIssuer, jwtAudience);
        mockMvc.perform(get("/api/v1/notifications/other").param("token", token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void streamWithValidQueryTokenStartsSseStream() throws Exception {
        String token = mintToken("42", Instant.now(), Instant.now().plusSeconds(300),
                jwtIssuer, jwtAudience);
        mockMvc.perform(get(STREAM_URL).param("token", token))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted());
    }

    @Test
    void streamWithValidBearerHeaderStartsSseStream() throws Exception {
        String token = mintToken("42", Instant.now(), Instant.now().plusSeconds(300),
                jwtIssuer, jwtAudience);
        mockMvc.perform(get(STREAM_URL).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted());
    }

    private String mintToken(String subject, Instant issuedAt, Instant expiresAt, String issuer, String audience) {
        SecretKey key = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        org.springframework.security.oauth2.jwt.NimbusJwtEncoder encoder = org.springframework.security.oauth2.jwt.NimbusJwtEncoder.withSecretKey(key)
                .algorithm(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS512)
                .build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(subject)
                .audience(List.of(audience))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
        return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
