package com.lodhi.auth.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry for managing SSE emitters per user
 * Supports multiple connections per user (multiple tabs/devices)
 */
@Component
@Slf4j
public class SseEmitterRegistry {

    // Map: userId -> List of emitters (multiple tabs per user)
    private final Map<Long, List<SseEmitter>> userEmitters = new ConcurrentHashMap<>();

    /**
     * Register a new emitter for a user
     */
    public void addEmitter(Long userId, SseEmitter emitter) {
        userEmitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        
        // Setup cleanup on emitter completion/timeout/error
        Runnable cleanup = () -> removeEmitter(userId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());
        
        log.info("SSE emitter registered for userId={}, total connections={}", 
            userId, userEmitters.get(userId).size());
    }

    /**
     * Remove a specific emitter for a user
     */
    public void removeEmitter(Long userId, SseEmitter emitter) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                userEmitters.remove(userId);
            }
            log.info("SSE emitter removed for userId={}, remaining connections={}", 
                userId, emitters.size());
        }
    }

    /**
     * Send event to all emitters for a specific user
     * Returns the number of successful sends
     */
    public int sendToUser(Long userId, SseEmitter.SseEventBuilder event) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            log.debug("No SSE connections for userId={}", userId);
            return 0;
        }

        int successCount = 0;
        List<SseEmitter> failedEmitters = new CopyOnWriteArrayList<>();

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(event);
                successCount++;
            } catch (IOException e) {
                log.warn("Failed to send SSE event to userId={}, removing dead emitter", userId, e);
                failedEmitters.add(emitter);
            }
        }

        // Clean up failed emitters
        failedEmitters.forEach(emitter -> removeEmitter(userId, emitter));

        log.debug("Sent SSE event to userId={}, successful={}, failed={}", 
            userId, successCount, failedEmitters.size());
        
        return successCount;
    }

    /**
     * Broadcast event to all connected users
     */
    public void broadcastToAll(SseEmitter.SseEventBuilder event) {
        log.info("Broadcasting SSE event to {} users", userEmitters.size());
        userEmitters.keySet().forEach(userId -> sendToUser(userId, event));
    }

    /**
     * Get active connection count for a user
     */
    public int getConnectionCount(Long userId) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        return emitters != null ? emitters.size() : 0;
    }

    /**
     * Get total number of connected users
     */
    public int getTotalUsers() {
        return userEmitters.size();
    }

    /**
     * Get total number of active connections
     */
    public int getTotalConnections() {
        return userEmitters.values().stream()
                .mapToInt(List::size)
                .sum();
    }
}
