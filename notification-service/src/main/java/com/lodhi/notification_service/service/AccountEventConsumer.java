package com.lodhi.notification_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lodhi.notification.contract.events.AccountEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountEventConsumer {

    private final SseConnectionManager sseConnectionManager;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "account-events", groupId = "notification-group")
    public void handleAccountEvent(AccountEvent event) {
        log.info("Received AccountEvent for userId: {}", event.getUserId());

        String eventData;
        try {
            eventData = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize account event: eventId=" + event.getEventId(), e);
        }

        int sent = sseConnectionManager.sendToUser(event.getUserId(), () -> {
            SseEmitter.SseEventBuilder builder = SseEmitter.event()
                    .name(event.getType().name())
                    .data(eventData)
                    .reconnectTime(5000);
            if (event.getEventId() != null) {
                builder.id(event.getEventId());
            }
            return builder;
        });

        if (sent > 0) {
            log.info("Pushed event '{}' to {} SSE connection(s) of userId: {}", event.getType(), sent, event.getUserId());
        } else {
            log.debug("User {} not connected, event buffered for replay", event.getUserId());
        }

        // Buffer for offline users and reconnects; captured by the stream's
        // replay logic when the client passes its Last-Event-ID.
        String redisKey = SseConnectionManager.missedEventsKey(event.getUserId());
        redisTemplate.opsForList().rightPush(redisKey, event);
        redisTemplate.opsForList().trim(redisKey, -50, -1);
        redisTemplate.expire(redisKey, Duration.ofDays(7));
    }
}
