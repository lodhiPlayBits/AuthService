package com.lodhi.notification_service.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lodhi.notification_service.service.AnnouncementProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.Instant;

/**
 * Handles announcement STOMP frames on {@code /app/announcement.*}.
 *
 * <ul>
 *   <li>{@code /app/announcement.send} — admin-only; publishes to the Kafka
 *       {@code broadcast-announcements} topic (the consumer fans out to
 *       {@code /topic/announcements})</li>
 *   <li>{@code /app/announcement.ack} / {@code .dismiss} — confirms to the acting
 *       user's {@code /user/queue/confirmations} and notifies admins on
 *       {@code /topic/admin.responses}</li>
 * </ul>
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@Profile("!test") // STOMP broker beans only exist with WebSocketConfig (web environment)
public class AnnouncementStompController {

    public static final int MAX_TITLE_LENGTH = 100;
    public static final int MAX_MESSAGE_LENGTH = 2000;

    private final AnnouncementProducer announcementProducer;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final org.springframework.kafka.core.KafkaTemplate<String, Object> announcementKafkaTemplate;

    @MessageMapping("/announcement.send")
    public void sendAnnouncement(@Payload AnnouncementRequest request, Principal principal) {
        if (!(principal instanceof StompPrincipal stompPrincipal) || !stompPrincipal.isAdmin()) {
            sendError(principal, "Unauthorized to send announcements");
            return;
        }
        if (request == null) {
            sendError(principal, "Payload is required");
            return;
        }

        String title = request.title() == null ? "" : request.title();
        String message = request.message() == null ? "" : request.message();
        String priority = (request.priority() == null || request.priority().isBlank())
                ? "NORMAL" : request.priority();

        if (title.length() > MAX_TITLE_LENGTH || message.length() > MAX_MESSAGE_LENGTH) {
            sendError(principal, "Title or message exceeds maximum allowed length");
            return;
        }

        announcementProducer.publishAnnouncement(title, message, priority, stompPrincipal.userId());
        log.info("Admin {} requested to broadcast announcement: {}", stompPrincipal.userId(), title);
    }

    @MessageMapping("/announcement.ack")
    public void acknowledgeAnnouncement(@Payload AnnouncementActionRequest request, Principal principal) {
        handleUserResponse(principal, request == null ? null : request.announcementId(), "ACK_ANNOUNCEMENT");
    }

    @MessageMapping("/announcement.dismiss")
    public void dismissAnnouncement(@Payload AnnouncementActionRequest request, Principal principal) {
        handleUserResponse(principal, request == null ? null : request.announcementId(), "DISMISS_ANNOUNCEMENT");
    }

    private void handleUserResponse(Principal principal, String announcementId, String action) {
        if (!(principal instanceof StompPrincipal stompPrincipal)) {
            log.warn("Ignoring announcement response from unauthenticated session");
            return;
        }
        if (announcementId == null || announcementId.isBlank()) {
            sendError(principal, "announcementId is required");
            return;
        }

        boolean ack = "ACK_ANNOUNCEMENT".equals(action);

        java.util.Map<String, Object> confirmation = new java.util.HashMap<>();
        confirmation.put("type", ack ? "ACK_CONFIRMED" : "DISMISS_CONFIRMED");
        confirmation.put("announcementId", announcementId);
        messagingTemplate.convertAndSendToUser(
                stompPrincipal.getName(), StompDestinations.CONFIRMATIONS_QUEUE, (Object) confirmation);

        java.util.Map<String, Object> adminMsg = new java.util.HashMap<>();
        adminMsg.put("type", "USER_RESPONSE");
        adminMsg.put("announcementId", announcementId);
        adminMsg.put("userId", stompPrincipal.userId());
        adminMsg.put("responseAction", ack ? "ACK" : "DISMISS");
        adminMsg.put("timestamp", Instant.now().toString());
        announcementKafkaTemplate.send(com.lodhi.notification_service.config.KafkaAnnouncementConfig.ADMIN_RESPONSES_TOPIC, announcementId, adminMsg);

        log.debug("User {} responded with {} to announcement {}",
                stompPrincipal.userId(), action, announcementId);
    }

    private void sendError(Principal principal, String errorMsg) {
        if (!(principal instanceof StompPrincipal stompPrincipal)) {
            return;
        }
        java.util.Map<String, Object> error = new java.util.HashMap<>();
        error.put("type", "ERROR");
        error.put("message", errorMsg);
        messagingTemplate.convertAndSendToUser(
                stompPrincipal.getName(), StompDestinations.ERRORS_QUEUE, (Object) error);
    }

    public record AnnouncementRequest(String title, String message, String priority) {
    }

    public record AnnouncementActionRequest(String announcementId) {
    }
}
