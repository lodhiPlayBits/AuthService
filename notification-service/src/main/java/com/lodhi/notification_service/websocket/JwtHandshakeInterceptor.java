package com.lodhi.notification_service.websocket;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Intercepts WebSocket handshakes and authenticates the upgrade via the JWT
 * that rides in the {@code token} query parameter (browsers cannot attach
 * headers to WebSocket handshakes).
 *
 * <p>Validation delegates to the same {@link JwtDecoder} the resource server
 * uses for the SSE stream, so signature, expiration, issuer and audience rules
 * are identical across both realtime channels. Like the SSE stream, there is no
 * Redis blacklist lookup here — revocation reaches this service as events.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    public static final String JWT_ATTRIBUTE = "JWT";

    private final JwtDecoder jwtDecoder;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {

        String token = extractTokenFromQuery(request.getURI().getQuery());

        if (token == null) {
            log.warn("WebSocket handshake failed: No token provided in query parameters");
            return false;
        }

        try {
            Jwt jwt = jwtDecoder.decode(token);

            // Only short-lived access tokens may open WebSocket sessions;
            // refresh tokens are single-purpose and never authenticate here
            if (!"access".equals(jwt.getClaimAsString("type"))) {
                log.warn("WebSocket handshake failed: token is not an access token");
                return false;
            }

            attributes.put(JWT_ATTRIBUTE, jwt);
            log.info("WebSocket handshake successful for userId={}", jwt.getSubject());
            return true;

        } catch (JwtException e) {
            log.warn("WebSocket handshake failed: invalid token: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // Nothing to do
    }

    private String extractTokenFromQuery(String query) {
        if (query == null) return null;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2 && keyValue[0].equals("token")) {
                return keyValue[1];
            }
        }
        return null;
    }
}
