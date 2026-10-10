package com.lodhi.notification_service.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Inbound STOMP frame authorization, run before any frame reaches the broker.
 *
 * <p>Two guards, both required:
 * <ul>
 *   <li>SUBSCRIBE to {@code /topic/admin**} requires the ADMIN role.</li>
 *   <li>SEND is restricted to application destinations ({@code /app/**}). The simple
 *       broker subscribes to the client inbound channel, so without this guard an
 *       authenticated client could SEND straight to {@code /topic/announcements} and
 *       have it re-broadcast to every subscriber, bypassing the Kafka pipeline and
 *       the admin check.</li>
 * </ul>
 */
@Component
@Slf4j
public class SubscriptionAuthorizationInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();
        if (command == null) {
            return message;
        }

        if (StompCommand.SUBSCRIBE.equals(command)) {
            validateSubscription(accessor);
        } else if (StompCommand.SEND.equals(command)) {
            validateSend(accessor);
        }

        return message;
    }

    private void validateSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination != null && destination.startsWith(StompDestinations.ADMIN_TOPIC_PREFIX) 
                && (!(accessor.getUser() instanceof StompPrincipal principal) || !principal.isAdmin())) {
            log.warn("Rejected admin-topic subscription from user={}",
                    accessor.getUser() != null ? accessor.getUser().getName() : "<anonymous>");
            throw new AccessDeniedException("Admin role required to subscribe to " + destination);
        }
    }

    private void validateSend(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null || !destination.startsWith(StompDestinations.APPLICATION_PREFIX + "/")) {
            throw new AccessDeniedException(
                    "Client sends are only allowed to " + StompDestinations.APPLICATION_PREFIX + "/** destinations");
        }
        if (accessor.getUser() == null) {
            throw new AccessDeniedException("Authenticated session required to send messages");
        }
    }
}
