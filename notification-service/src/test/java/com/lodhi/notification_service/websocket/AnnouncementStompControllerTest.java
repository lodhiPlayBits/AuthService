package com.lodhi.notification_service.websocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lodhi.notification_service.service.AnnouncementProducer;
import com.lodhi.notification_service.websocket.AnnouncementStompController.AnnouncementActionRequest;
import com.lodhi.notification_service.websocket.AnnouncementStompController.AnnouncementRequest;

@ExtendWith(MockitoExtension.class)
class AnnouncementStompControllerTest {

    private static final StompPrincipal ADMIN = new StompPrincipal(1L, List.of("USER", "ADMIN"));
    private static final StompPrincipal USER = new StompPrincipal(2L, List.of("USER"));

    @Mock private AnnouncementProducer announcementProducer;
    @Mock private SimpMessagingTemplate messagingTemplate;

    private AnnouncementStompController controller;

    @BeforeEach
    void setUp() {
        controller = new AnnouncementStompController(announcementProducer, messagingTemplate, new ObjectMapper());
    }

    @Test
    void sendAnnouncement_AsAdmin_PublishesToKafka() {
        controller.sendAnnouncement(new AnnouncementRequest("Maintenance", "Tonight 22:00", "HIGH"), ADMIN);

        verify(announcementProducer).publishAnnouncement("Maintenance", "Tonight 22:00", "HIGH", 1L);
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void sendAnnouncement_BlankPriority_DefaultsToNormal() {
        controller.sendAnnouncement(new AnnouncementRequest("Maintenance", "Tonight", "  "), ADMIN);

        verify(announcementProducer).publishAnnouncement("Maintenance", "Tonight", "NORMAL", 1L);
    }

    @Test
    void sendAnnouncement_NullFields_AreTreatedAsEmpty() {
        controller.sendAnnouncement(new AnnouncementRequest(null, null, null), ADMIN);

        verify(announcementProducer).publishAnnouncement("", "", "NORMAL", 1L);
    }

    @Test
    void sendAnnouncement_AsRegularUser_SendsErrorAndNeverPublishes() {
        controller.sendAnnouncement(new AnnouncementRequest("Maintenance", "Tonight", "HIGH"), USER);

        ObjectNode error = captureUserPayload("2", StompDestinations.ERRORS_QUEUE);
        assertEquals("ERROR", error.get("type").asText());
        assertEquals("Unauthorized to send announcements", error.get("message").asText());
        verifyNoInteractions(announcementProducer);
    }

    @Test
    void sendAnnouncement_WithoutPrincipal_DoesNothing() {
        controller.sendAnnouncement(new AnnouncementRequest("Maintenance", "Tonight", "HIGH"), null);

        verifyNoInteractions(announcementProducer, messagingTemplate);
    }

    @Test
    void sendAnnouncement_NullRequest_SendsError() {
        controller.sendAnnouncement(null, ADMIN);

        ObjectNode error = captureUserPayload("1", StompDestinations.ERRORS_QUEUE);
        assertEquals("Payload is required", error.get("message").asText());
        verifyNoInteractions(announcementProducer);
    }

    @Test
    void sendAnnouncement_OversizedTitle_SendsErrorAndNeverPublishes() {
        String longTitle = "x".repeat(AnnouncementStompController.MAX_TITLE_LENGTH + 1);

        controller.sendAnnouncement(new AnnouncementRequest(longTitle, "Tonight", "HIGH"), ADMIN);

        ObjectNode error = captureUserPayload("1", StompDestinations.ERRORS_QUEUE);
        assertEquals("Title or message exceeds maximum allowed length", error.get("message").asText());
        verifyNoInteractions(announcementProducer);
    }

    @Test
    void sendAnnouncement_OversizedMessage_SendsErrorAndNeverPublishes() {
        String longMessage = "x".repeat(AnnouncementStompController.MAX_MESSAGE_LENGTH + 1);

        controller.sendAnnouncement(new AnnouncementRequest("Maintenance", longMessage, "HIGH"), ADMIN);

        verifyNoInteractions(announcementProducer);
        verify(messagingTemplate, only()).convertAndSendToUser(eq("1"), eq(StompDestinations.ERRORS_QUEUE), any());
    }

    @Test
    void acknowledge_SendsConfirmationToUserAndNotifiesAdmins() {
        controller.acknowledgeAnnouncement(new AnnouncementActionRequest("ann-1"), USER);

        ObjectNode confirmation = captureUserPayload("2", StompDestinations.CONFIRMATIONS_QUEUE);
        assertEquals("ACK_CONFIRMED", confirmation.get("type").asText());
        assertEquals("ann-1", confirmation.get("announcementId").asText());

        ObjectNode adminMsg = captureAdminPayload();
        assertEquals("USER_RESPONSE", adminMsg.get("type").asText());
        assertEquals("ann-1", adminMsg.get("announcementId").asText());
        assertEquals(2L, adminMsg.get("userId").asLong());
        assertEquals("ACK", adminMsg.get("responseAction").asText());
        assertNotNull(adminMsg.get("timestamp").asText());
    }

    @Test
    void dismiss_SendsConfirmationToUserAndNotifiesAdmins() {
        controller.dismissAnnouncement(new AnnouncementActionRequest("ann-2"), USER);

        ObjectNode confirmation = captureUserPayload("2", StompDestinations.CONFIRMATIONS_QUEUE);
        assertEquals("DISMISS_CONFIRMED", confirmation.get("type").asText());

        ObjectNode adminMsg = captureAdminPayload();
        assertEquals("DISMISS", adminMsg.get("responseAction").asText());
    }

    @Test
    void acknowledge_BlankId_SendsErrorOnly() {
        controller.acknowledgeAnnouncement(new AnnouncementActionRequest("  "), USER);

        ObjectNode error = captureUserPayload("2", StompDestinations.ERRORS_QUEUE);
        assertEquals("announcementId is required", error.get("message").asText());
        verify(messagingTemplate, only()).convertAndSendToUser(eq("2"), eq(StompDestinations.ERRORS_QUEUE), any());
    }

    @Test
    void acknowledge_NullRequest_SendsError() {
        controller.acknowledgeAnnouncement(null, USER);

        verify(messagingTemplate, only()).convertAndSendToUser(eq("2"), eq(StompDestinations.ERRORS_QUEUE), any());
    }

    @Test
    void acknowledge_WithoutPrincipal_IsIgnored() {
        controller.acknowledgeAnnouncement(new AnnouncementActionRequest("ann-1"), null);

        verifyNoInteractions(messagingTemplate);
    }

    private ObjectNode captureUserPayload(String user, String destination) {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSendToUser(eq(user), eq(destination), captor.capture());
        return (ObjectNode) captor.getValue();
    }

    private ObjectNode captureAdminPayload() {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(eq(StompDestinations.ADMIN_RESPONSES_TOPIC), captor.capture());
        return (ObjectNode) captor.getValue();
    }
}
