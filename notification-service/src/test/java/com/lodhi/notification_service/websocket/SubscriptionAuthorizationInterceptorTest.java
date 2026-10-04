package com.lodhi.notification_service.websocket;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.security.Principal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;

class SubscriptionAuthorizationInterceptorTest {

    private static final StompPrincipal ADMIN = new StompPrincipal(1L, List.of("USER", "ADMIN"), java.time.Instant.now().plusSeconds(3600));
    private static final StompPrincipal USER = new StompPrincipal(2L, List.of("USER"), java.time.Instant.now().plusSeconds(3600));

    private final SubscriptionAuthorizationInterceptor interceptor = new SubscriptionAuthorizationInterceptor();
    private final MessageChannel channel = mock(MessageChannel.class);

    @Test
    void subscribeToAdminTopic_AsRegularUser_IsDenied() {
        Message<byte[]> frame = stompFrame(StompCommand.SUBSCRIBE, StompDestinations.ADMIN_RESPONSES_TOPIC, USER);
        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(frame, channel));
    }

    @Test
    void subscribeToAdminTopic_WithoutPrincipal_IsDenied() {
        Message<byte[]> frame = stompFrame(StompCommand.SUBSCRIBE, StompDestinations.ADMIN_RESPONSES_TOPIC, null);
        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(frame, channel));
    }

    @Test
    void subscribeToAdminTopic_AsAdmin_IsAllowed() {
        Message<byte[]> frame = stompFrame(StompCommand.SUBSCRIBE, StompDestinations.ADMIN_RESPONSES_TOPIC, ADMIN);
        assertSame(frame, interceptor.preSend(frame, channel));
    }

    @Test
    void subscribeToAnnouncementsTopic_AsRegularUser_IsAllowed() {
        Message<byte[]> frame = stompFrame(StompCommand.SUBSCRIBE, StompDestinations.ANNOUNCEMENTS_TOPIC, USER);
        assertSame(frame, interceptor.preSend(frame, channel));
    }

    @Test
    void subscribeToUserQueue_IsAllowed() {
        Message<byte[]> frame = stompFrame(StompCommand.SUBSCRIBE, "/user/queue/confirmations", USER);
        assertSame(frame, interceptor.preSend(frame, channel));
    }

    @Test
    void sendToTopicDestination_IsDenied_EvenForAdmin() {
        // Without this guard the simple broker would rebroadcast the frame,
        // bypassing the Kafka pipeline and the admin check.
        Message<byte[]> frame = stompFrame(StompCommand.SEND, StompDestinations.ANNOUNCEMENTS_TOPIC, ADMIN);
        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(frame, channel));
    }

    @Test
    void sendToApplicationDestination_WithPrincipal_IsAllowed() {
        Message<byte[]> frame = stompFrame(StompCommand.SEND, "/app/announcement.send", ADMIN);
        assertSame(frame, interceptor.preSend(frame, channel));
    }

    @Test
    void sendToApplicationDestination_WithoutPrincipal_IsDenied() {
        Message<byte[]> frame = stompFrame(StompCommand.SEND, "/app/announcement.ack", null);
        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(frame, channel));
    }

    @Test
    void sendWithoutDestination_IsDenied() {
        Message<byte[]> frame = stompFrame(StompCommand.SEND, null, USER);
        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(frame, channel));
    }

    @Test
    void connectFrame_IsUntouched() {
        Message<byte[]> frame = stompFrame(StompCommand.CONNECT, null, null);
        assertSame(frame, interceptor.preSend(frame, channel));
    }

    @Test
    void nonStompMessage_IsReturnedUnchanged() {
        Message<String> message = MessageBuilder.withPayload("not-stomp").build();
        assertSame(message, interceptor.preSend(message, channel));
    }

    private Message<byte[]> stompFrame(StompCommand command, String destination, Principal user) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        if (destination != null) {
            accessor.setDestination(destination);
        }
        if (user != null) {
            accessor.setUser(user);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
