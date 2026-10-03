package com.lodhi.notification_service.websocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.web.socket.WebSocketHandler;

/**
 * Exercises the real handshake interceptor against a real {@link JwtDecoder}
 * built exactly like {@code SecurityConfig#jwtDecoder} and tokens minted with
 * {@link NimbusJwtEncoder} — same signature/expiry/issuer/audience rules as the
 * SSE resource server, plus the access-token type check.
 */
class JwtHandshakeInterceptorTest {

    private static final String SECRET = "test-secret-key-that-is-at-least-32-bytes-long-for-hmac-sha256";
    private static final String ISSUER = "test-issuer";
    private static final String AUDIENCE = "test-audience";

    private final ServerHttpResponse response = mock(ServerHttpResponse.class);
    private final WebSocketHandler wsHandler = mock(WebSocketHandler.class);
    private final Map<String, Object> attributes = new HashMap<>();

    private JwtHandshakeInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new JwtHandshakeInterceptor(realDecoder());
    }

    @Test
    void beforeHandshake_MissingToken_ReturnsFalse() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/ws/announcements");

        boolean accepted = interceptor.beforeHandshake(
                new ServletServerHttpRequest(servletRequest), response, wsHandler, attributes);

        assertFalse(accepted);
        assertTrue(attributes.isEmpty());
    }

    @Test
    void beforeHandshake_ValidAccessToken_ReturnsTrueAndStoresJwt() {
        String token = mintToken("42", "access", Instant.now(), Instant.now().plusSeconds(60),
                ISSUER, AUDIENCE);

        boolean accepted = interceptor.beforeHandshake(
                requestWithToken(token), response, wsHandler, attributes);

        assertTrue(accepted);
        Jwt jwt = assertInstanceOf(Jwt.class, attributes.get(JwtHandshakeInterceptor.JWT_ATTRIBUTE));
        assertEquals("42", jwt.getSubject());
        assertEquals(List.of("ADMIN"), jwt.getClaim("roles"));
    }

    @Test
    void beforeHandshake_RefreshToken_ReturnsFalse() {
        String token = mintToken("42", "refresh", Instant.now(), Instant.now().plusSeconds(60),
                ISSUER, AUDIENCE);

        boolean accepted = interceptor.beforeHandshake(
                requestWithToken(token), response, wsHandler, attributes);

        assertFalse(accepted);
        assertTrue(attributes.isEmpty());
    }

    @Test
    void beforeHandshake_MissingTypeClaim_ReturnsFalse() {
        // Tokens without a type claim (e.g. legacy minting) must not open WS sessions
        String token = mintToken("42", null, Instant.now(), Instant.now().plusSeconds(60),
                ISSUER, AUDIENCE);

        boolean accepted = interceptor.beforeHandshake(
                requestWithToken(token), response, wsHandler, attributes);

        assertFalse(accepted);
        assertTrue(attributes.isEmpty());
    }

    @Test
    void beforeHandshake_ExpiredToken_ReturnsFalse() {
        String token = mintToken("42", "access", Instant.now().minusSeconds(600), Instant.now().minusSeconds(300),
                ISSUER, AUDIENCE);

        boolean accepted = interceptor.beforeHandshake(
                requestWithToken(token), response, wsHandler, attributes);

        assertFalse(accepted);
        assertTrue(attributes.isEmpty());
    }

    @Test
    void beforeHandshake_WrongIssuer_ReturnsFalse() {
        String token = mintToken("42", "access", Instant.now(), Instant.now().plusSeconds(60),
                "some-other-issuer", AUDIENCE);

        boolean accepted = interceptor.beforeHandshake(
                requestWithToken(token), response, wsHandler, attributes);

        assertFalse(accepted);
        assertTrue(attributes.isEmpty());
    }

    @Test
    void beforeHandshake_WrongAudience_ReturnsFalse() {
        String token = mintToken("42", "access", Instant.now(), Instant.now().plusSeconds(60),
                ISSUER, "some-other-audience");

        boolean accepted = interceptor.beforeHandshake(
                requestWithToken(token), response, wsHandler, attributes);

        assertFalse(accepted);
        assertTrue(attributes.isEmpty());
    }

    @Test
    void beforeHandshake_GarbageToken_ReturnsFalse() {
        boolean accepted = interceptor.beforeHandshake(
                requestWithToken("not-a-jwt"), response, wsHandler, attributes);

        assertFalse(accepted);
        assertTrue(attributes.isEmpty());
    }

    private ServletServerHttpRequest requestWithToken(String token) {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/ws/announcements");
        servletRequest.setQueryString("token=" + token);
        return new ServletServerHttpRequest(servletRequest);
    }

    private JwtDecoder realDecoder() {
        SecretKey key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(ISSUER),
                new JwtClaimValidator<List<String>>(JwtClaimNames.AUD,
                        audiences -> audiences != null && audiences.contains(AUDIENCE))));
        return decoder;
    }

    private String mintToken(String subject, String type, Instant issuedAt, Instant expiresAt,
                             String issuer, String audience) {
        SecretKey key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        NimbusJwtEncoder encoder = NimbusJwtEncoder.withSecretKey(key)
                .algorithm(MacAlgorithm.HS256)
                .build();

        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(subject)
                .audience(List.of(audience))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("roles", List.of("ADMIN"));
        if (type != null) {
            claims.claim("type", type);
        }
        return encoder.encode(JwtEncoderParameters.from(claims.build())).getTokenValue();
    }
}
