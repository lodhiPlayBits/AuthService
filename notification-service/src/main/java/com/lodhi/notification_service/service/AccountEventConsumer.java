package com.lodhi.notification_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountEventConsumer {

    private final SseConnectionManager sseConnectionManager;

    @KafkaListener(topics = "account-events", groupId = "notification-group")
    public void handleAccountEvent(AccountEvent event) {
        log.info("Received AccountEvent for userId: {}", event.getUserId());
        
        // Push the event to the user if they are currently connected via SSE
        sseConnectionManager.sendMessageToUser(
                event.getUserId(), 
                event.getType().name(), 
                event
        );
    }
}
