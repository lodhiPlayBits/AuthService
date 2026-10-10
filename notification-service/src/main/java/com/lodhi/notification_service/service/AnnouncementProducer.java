package com.lodhi.notification_service.service;

import com.lodhi.notification.contract.events.BroadcastAnnouncementEvent;
import com.lodhi.notification_service.config.KafkaAnnouncementConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Publishes broadcast announcements to Kafka; the WebSocket consumer fans them
 * out to {@code /topic/announcements}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnnouncementProducer {

    private final KafkaTemplate<String, Object> announcementKafkaTemplate;

    public BroadcastAnnouncementEvent publishAnnouncement(String title, String message, String priority, Long adminId) {
        BroadcastAnnouncementEvent event = BroadcastAnnouncementEvent.builder()
                .announcementId(UUID.randomUUID().toString())
                .title(title)
                .message(message)
                .priority(priority)
                .sentBy(adminId)
                .timestamp(Instant.now())
                .build();

        announcementKafkaTemplate.send(KafkaAnnouncementConfig.BROADCAST_ANNOUNCEMENTS_TOPIC, event.getAnnouncementId(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Successfully published broadcast announcement: {}", event.getAnnouncementId());
                    } else {
                        log.error("Failed to publish broadcast announcement", ex);
                    }
                });

        return event;
    }
}
