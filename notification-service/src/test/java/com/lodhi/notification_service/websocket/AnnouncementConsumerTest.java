package com.lodhi.notification_service.websocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lodhi.notification.contract.events.BroadcastAnnouncementEvent;

@ExtendWith(MockitoExtension.class)
class AnnouncementConsumerTest {

    @Mock private SimpMessagingTemplate messagingTemplate;

    private AnnouncementConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new AnnouncementConsumer(messagingTemplate, new ObjectMapper());
    }

    @Test
    void handleBroadcastAnnouncement_PublishFailure_PropagatesAndStaysRetryable() {
        doThrow(new MessagingException("boom"))
                .when(messagingTemplate).convertAndSend(eq(StompDestinations.ANNOUNCEMENTS_TOPIC), any(Object.class));

        BroadcastAnnouncementEvent event = announcement("ann-1");

        assertThrows(MessagingException.class, () -> consumer.handleBroadcastAnnouncement(event));

        // Redelivery after a failure must actually publish again, not be deduped away
        assertThrows(MessagingException.class, () -> consumer.handleBroadcastAnnouncement(event));
        verify(messagingTemplate, times(2)).convertAndSend(eq(StompDestinations.ANNOUNCEMENTS_TOPIC), any(Object.class));
    }

    @Test
    void handleBroadcastAnnouncement_FailureThenRedelivery_ReprocessesInsteadOfSkippingAsDuplicate() {
        doThrow(new MessagingException("boom"))
                .doNothing()
                .when(messagingTemplate).convertAndSend(eq(StompDestinations.ANNOUNCEMENTS_TOPIC), any(Object.class));

        BroadcastAnnouncementEvent event = announcement("ann-1");

        assertThrows(MessagingException.class, () -> consumer.handleBroadcastAnnouncement(event));

        // Redelivery after a failure must actually process...
        consumer.handleBroadcastAnnouncement(event);
        verify(messagingTemplate, times(2)).convertAndSend(eq(StompDestinations.ANNOUNCEMENTS_TOPIC), any(Object.class));

        // ...and only then is the announcement treated as a duplicate
        consumer.handleBroadcastAnnouncement(event);
        verify(messagingTemplate, times(2)).convertAndSend(eq(StompDestinations.ANNOUNCEMENTS_TOPIC), any(Object.class));
    }

    @Test
    void handleBroadcastAnnouncement_DuplicateAfterSuccess_IsSkipped() {
        BroadcastAnnouncementEvent event = announcement("ann-1");

        consumer.handleBroadcastAnnouncement(event);
        consumer.handleBroadcastAnnouncement(event);

        verify(messagingTemplate, times(1)).convertAndSend(eq(StompDestinations.ANNOUNCEMENTS_TOPIC), any(Object.class));
    }

    @Test
    void handleBroadcastAnnouncement_PublishesExpectedPayloadToAnnouncementsTopic() {
        BroadcastAnnouncementEvent event = announcement("ann-1");

        consumer.handleBroadcastAnnouncement(event);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(eq(StompDestinations.ANNOUNCEMENTS_TOPIC), payloadCaptor.capture());

        java.util.Map<String, Object> payload = (java.util.Map<String, Object>) payloadCaptor.getValue();
        assertEquals("ANNOUNCEMENT", payload.get("type").toString());
        assertEquals("ann-1", payload.get("announcementId").toString());
        assertEquals("Maintenance", payload.get("title").toString());
        assertEquals("Scheduled maintenance tonight", payload.get("message").toString());
        assertEquals("HIGH", payload.get("priority").toString());
        assertEquals(1L, Long.parseLong(payload.get("sentBy").toString()));
        assertNotNull(payload.get("timestamp").toString());
    }

    @Test
    void cleanupDeduplicationCache_KeepsRecentEntries() {
        BroadcastAnnouncementEvent event = announcement("ann-1");

        consumer.handleBroadcastAnnouncement(event);
        consumer.cleanupDeduplicationCache();
        consumer.handleBroadcastAnnouncement(event);

        verify(messagingTemplate, times(1)).convertAndSend(eq(StompDestinations.ANNOUNCEMENTS_TOPIC), any(Object.class));
    }

    private BroadcastAnnouncementEvent announcement(String announcementId) {
        return BroadcastAnnouncementEvent.builder()
                .announcementId(announcementId)
                .title("Maintenance")
                .message("Scheduled maintenance tonight")
                .priority("HIGH")
                .sentBy(1L)
                .timestamp(Instant.now())
                .build();
    }
}
