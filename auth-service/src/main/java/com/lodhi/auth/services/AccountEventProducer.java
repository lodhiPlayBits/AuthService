package com.lodhi.auth.services;

import com.lodhi.auth.config.KafkaTopicConfig;
import com.lodhi.notification.contract.events.AccountEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service for publishing account events to Kafka
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountEventProducer {

    private final KafkaTemplate<String, AccountEvent> kafkaTemplate;

    /**
     * Publish account event with userId as partition key for ordering
     */
    public void publishAccountEvent(AccountEvent.AccountEventType type, Long userId, String email, String message, Object payload) {
        AccountEvent event = AccountEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .type(type)
                .userId(userId)
                .email(email)
                .message(message)
                .payload(payload)
                .timestamp(Instant.now())
                .build();

        // Use userId as key to ensure all events for a user go to the same partition (ordering)
        String key = String.valueOf(userId);
        
        CompletableFuture<SendResult<String, AccountEvent>> future = 
            kafkaTemplate.send(KafkaTopicConfig.ACCOUNT_EVENTS_TOPIC, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Published account event: type={}, userId={}, eventId={}, partition={}", 
                    type, userId, event.getEventId(), result.getRecordMetadata().partition());
            } else {
                log.error("Failed to publish account event: type={}, userId={}, eventId={}", 
                    type, userId, event.getEventId(), ex);
            }
        });
    }

    public void publishAccountDisabled(Long userId, String email, String reason) {
        publishAccountEvent(
            AccountEvent.AccountEventType.ACCOUNT_DISABLED,
            userId,
            email,
            "Your account has been disabled" + (reason != null ? ": " + reason : ""),
            null
        );
    }

    public void publishAccountEnabled(Long userId, String email) {
        publishAccountEvent(
            AccountEvent.AccountEventType.ACCOUNT_ENABLED,
            userId,
            email,
            "Your account has been enabled",
            null
        );
    }

    public void publishSessionRevoked(Long userId, String email) {
        publishAccountEvent(
            AccountEvent.AccountEventType.SESSION_REVOKED,
            userId,
            email,
            "Your session has been revoked",
            null
        );
    }
}
