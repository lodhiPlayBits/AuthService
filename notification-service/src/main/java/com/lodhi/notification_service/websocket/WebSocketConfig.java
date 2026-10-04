package com.lodhi.notification_service.websocket;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import lombok.RequiredArgsConstructor;

/**
 * STOMP over WebSocket configuration.
 *
 * <p>Endpoint: {@code /ws/announcements?token=<access-token>} — the JWT handshake
 * interceptor authenticates the upgrade, and the handshake handler exposes the user
 * to the STOMP session so the broker can address {@code /user/**} destinations.
 *
 * <p>Destinations (see {@link StompDestinations}):
 * <ul>
 *   <li>{@code /topic/announcements} — broadcast announcements fed by Kafka</li>
 *   <li>{@code /topic/admin.responses} — admin-only user responses</li>
 *   <li>{@code /user/queue/confirmations}, {@code /user/queue/errors} — per-user queues</li>
 *   <li>{@code /app/announcement.send|ack|dismiss} — client actions</li>
 * </ul>
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Profile("!test") // MOCK web environment has no jakarta.websocket ServerContainer attribute
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;
    private final StompHandshakeHandler stompHandshakeHandler;
    private final SubscriptionAuthorizationInterceptor subscriptionAuthorizationInterceptor;
    private final WebSocketSessionManager webSocketSessionManager;

    @Value("${websocket.allowed-origins}")
    private String allowedOrigins;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/announcements")
                .setAllowedOrigins(resolveAllowedOrigins(allowedOrigins))
                .addInterceptors(jwtHandshakeInterceptor)
                .setHandshakeHandler(stompHandshakeHandler);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker(StompDestinations.TOPIC_PREFIX, StompDestinations.QUEUE_PREFIX)
                .setHeartbeatValue(new long[]{10000, 10000})
                .setTaskScheduler(websocketHeartbeatScheduler());
        registry.setApplicationDestinationPrefixes(StompDestinations.APPLICATION_PREFIX);
        registry.setUserDestinationPrefix(StompDestinations.USER_PREFIX);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(subscriptionAuthorizationInterceptor);
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        // Serializes sends per session so broker heartbeats, Kafka broadcasts and
        // request-handling threads can never write to one connection concurrently.
        registration.addDecoratorFactory(handler -> new ConcurrentSendWebSocketHandlerDecorator(handler, webSocketSessionManager));
    }

    @Bean
    public ThreadPoolTaskScheduler websocketHeartbeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        return scheduler;
    }

    /**
     * WebSocket handshakes are not protected by the browser same-origin policy,
     * so an explicit origin allow-list is mandatory. A wildcard would let any
     * website open an authenticated cross-site WebSocket (CSWSH) using the
     * victim's browser connection.
     */
    static String[] resolveAllowedOrigins(String configured) {
        if (configured == null || configured.isBlank()) {
            throw new IllegalStateException(
                    "websocket.allowed-origins must be an explicit list of origins");
        }
        String[] origins = Arrays.stream(configured.split(","))
                .map(String::trim)
                .toArray(String[]::new);
        if (Arrays.stream(origins).anyMatch(origin -> origin.isEmpty() || "*".equals(origin))) {
            throw new IllegalStateException(
                    "websocket.allowed-origins must not contain '*' or blank entries");
        }
        return origins;
    }

    /**
     * Configure the underlying WebSocket engine (Tomcat/Jetty)
     */
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        // Set maximum idle timeout to 5 minutes (connection closes if no message/ping for 5m)
        container.setMaxSessionIdleTimeout(300000L);
        // Set limits for incoming messages (16KB)
        container.setMaxTextMessageBufferSize(16384);
        container.setMaxBinaryMessageBufferSize(16384);
        return container;
    }
}
