package com.lodhi.notification_service.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lodhi.notification.contract.events.BroadcastAnnouncementEvent;
import com.lodhi.notification_service.config.KafkaAnnouncementConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Consumes broadcast announcements from Kafka and publishes them to the STOMP
 * {@code /topic/announcements} destination, where every subscribed client receives them.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!test") // STOMP broker beans only exist with WebSocketConfig (web environment)
public class AnnouncementConsumer {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    // Deduplication cache: announcementId -> processed timestamp
    private final Map<String, Instant> processedAnnouncements = new ConcurrentHashMap<>();

    @KafkaListener(
        topics = KafkaAnnouncementConfig.BROADCAST_ANNOUNCEMENTS_TOPIC,
        containerFactory = "announcementKafkaListenerContainerFactory"
    )
    public void handleBroadcastAnnouncement(BroadcastAnnouncementEvent event) {
        if (event.getAnnouncementId() != null && processedAnnouncements.containsKey(event.getAnnouncementId())) {
            log.debug("Skipping duplicate announcement: {}", event.getAnnouncementId());
            return;
        }

        log.info("Received broadcast announcement from Kafka: {}", event.getTitle());

        try {
            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("type", "ANNOUNCEMENT");
            payload.put("announcementId", event.getAnnouncementId());
            payload.put("title", event.getTitle());
            payload.put("message", event.getMessage());
            payload.put("priority", event.getPriority());
            payload.put("sentBy", event.getSentBy());
            payload.put("timestamp", event.getTimestamp().toString());

            messagingTemplate.convertAndSend(StompDestinations.ANNOUNCEMENTS_TOPIC, (Object) payload);
            log.info("Published announcement {} to {}", event.getAnnouncementId(), StompDestinations.ANNOUNCEMENTS_TOPIC);

            // Mark only on success — a failed announcement must stay retryable, not
            // be skipped as a duplicate when the error handler redelivers it
            if (event.getAnnouncementId() != null) {
                processedAnnouncements.put(event.getAnnouncementId(), Instant.now());
            }

        } catch (Exception e) {
            log.error("Error processing broadcast announcement", e);
            // Propagate so the container error handler retries (1s x3) and then dead-letters
            if (e instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Failed to process broadcast announcement: id=" + event.getAnnouncementId(), e);
        }
    }

    /**
     * Clean up deduplication cache periodically (every 1 hour)
     * Removes announcements older than 24 hours
     */
    @Scheduled(fixedRate = 3600000)
    public void cleanupDeduplicationCache() {
        Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS);
        int initialSize = processedAnnouncements.size();
        processedAnnouncements.values().removeIf(timestamp -> timestamp.isBefore(cutoff));
        int removed = initialSize - processedAnnouncements.size();
        if (removed > 0) {
            log.debug("Cleaned up {} expired announcements from deduplication cache", removed);
        }
    }

    @KafkaListener(
        topics = KafkaAnnouncementConfig.ADMIN_RESPONSES_TOPIC,
        containerFactory = "adminResponseKafkaListenerContainerFactory"
    )
    public void handleAdminResponse(Map<String, Object> event) {
        log.info("Received admin response from Kafka");
        try {
            messagingTemplate.convertAndSend(StompDestinations.ADMIN_RESPONSES_TOPIC, (Object) event);
            log.info("Published admin response to {}", StompDestinations.ADMIN_RESPONSES_TOPIC);
        } catch (Exception e) {
            log.error("Failed to process and broadcast admin response", e);
        }
    }
}
