package com.lodhi.notification_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class SseConnectionManager {

    // Store active connections mapped by userId
    private final Map<Long, SseEmitter> connections = new ConcurrentHashMap<>();

    public SseEmitter addConnection(Long userId) {
        // Create an emitter with no timeout (or a large timeout like 1 hour)
        SseEmitter emitter = new SseEmitter(3600000L);
        connections.put(userId, emitter);

        // Handle connection lifecycle
        emitter.onCompletion(() -> connections.remove(userId));
        emitter.onTimeout(() -> connections.remove(userId));
        emitter.onError(e -> connections.remove(userId));

        log.info("SSE connection established for userId: {}", userId);
        
        try {
            // Send an initial connected event
            emitter.send(SseEmitter.event().name("CONNECTED").data("Connected successfully"));
        } catch (IOException e) {
            connections.remove(userId);
        }

        return emitter;
    }

    public void sendMessageToUser(Long userId, String eventName, Object data) {
        SseEmitter emitter = connections.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
                log.info("Sent event '{}' to userId: {}", eventName, userId);
            } catch (IOException e) {
                log.error("Failed to send SSE event to userId: {}, removing connection", userId, e);
                connections.remove(userId);
            }
        } else {
            log.warn("No active SSE connection found for userId: {}", userId);
        }
    }
}
