package com.lodhi.notification_service.websocket;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

/**
 * Attaches the authenticated user to the STOMP session at handshake time.
 *
 * <p>The {@link JwtHandshakeInterceptor} validates the query-param token and stores the
 * decoded {@link Jwt} in the handshake attributes; this handler exposes it as the
 * {@link Principal} of the WebSocket session, which the STOMP layer then propagates to
 * every frame (CONNECT, SEND, SUBSCRIBE). That makes {@code SimpUserRegistry}
 * registration and {@code /user/**} destinations work.
 */
@Component
public class StompHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
                                      Map<String, Object> attributes) {
        if (attributes.get(JwtHandshakeInterceptor.JWT_ATTRIBUTE) instanceof Jwt jwt) {
            return new StompPrincipal(Long.parseLong(jwt.getSubject()), extractRoles(jwt), jwt.getExpiresAt());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRoles(Jwt jwt) {
        Object roles = jwt.getClaim("roles");
        return roles instanceof List ? (List<String>) roles : List.of();
    }
}
