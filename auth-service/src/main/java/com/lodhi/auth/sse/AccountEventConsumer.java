package com.lodhi.auth.sse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lodhi.auth.events.AccountEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Kafka consumer that receives account events and pushes them to SSE clients
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AccountEventConsumer {

    private final SseEmitterRegistry emitterRegistry;
    private final ObjectMapper objectMapper;

    /**
     * Listen to account events and push to connected SSE clients
     */
    @KafkaListener(
        topics = "account-events",
        containerFactory = "accountEventKafkaListenerContainerFactory"
    )
    public void handleAccountEvent(AccountEvent event) {
        log.info("Received account event: type={}, userId={}, eventId={}", 
            event.getType(), event.getUserId(), event.getEventId());

        try {
            // Convert event to JSON
            String eventData = objectMapper.writeValueAsString(event);

            // Create SSE event
            SseEmitter.SseEventBuilder sseEvent = SseEmitter.event()
                    .id(event.getEventId())
                    .name(event.getType().name())
                    .data(eventData)
                    .reconnectTime(5000); // Client should retry after 5 seconds

            // Send to the specific user
            int sentCount = emitterRegistry.sendToUser(event.getUserId(), sseEvent);
            
            if (sentCount > 0) {
                log.info("Pushed event to SSE clients: type={}, userId={}, connections={}", 
                    event.getType(), event.getUserId(), sentCount);
            } else {
                log.debug("User not connected, event not delivered: type={}, userId={}", 
                    event.getType(), event.getUserId());
            }

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize account event: eventId={}", event.getEventId(), e);
        }
    }
}
