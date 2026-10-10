package com.lodhi.notification_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

@Service
@Slf4j
public class SseConnectionManager {

    private static final int MAX_CONNECTIONS_PER_USER = 5;
    private static final int MAX_TOTAL_CONNECTIONS = 1000;
    private static final long EMITTER_TIMEOUT_MS = 3600000L;
    private static final String MISSED_EVENTS_KEY_PREFIX = "notification:v1:sse:missed:";

    // userId -> live emitters (multiple tabs/devices per user)
    private final Map<Long, List<SseEmitter>> connections = new ConcurrentHashMap<>();

    public static String missedEventsKey(Long userId) {
        return MISSED_EVENTS_KEY_PREFIX + userId;
    }

    /**
     * Registers a new emitter for the user. When a connection limit is hit the
     * returned emitter is already completed with an error instead of throwing,
     * so the caller simply hands it back to Spring MVC.
     */
    public SseEmitter addConnection(Long userId) {
        SseEmitter emitter = createEmitter();

        if (getTotalConnections() >= MAX_TOTAL_CONNECTIONS) {
            log.warn("Rejected SSE connection for userId={}: global limit of {} reached", userId, MAX_TOTAL_CONNECTIONS);
            emitter.completeWithError(new IllegalStateException("Global SSE connection limit reached"));
            return emitter;
        }

        List<SseEmitter> userEmitters = connections.computeIfAbsent(userId, key -> new CopyOnWriteArrayList<>());
        if (userEmitters.size() >= MAX_CONNECTIONS_PER_USER) {
            log.warn("Rejected SSE connection for userId={}: per-user limit of {} reached", userId, MAX_CONNECTIONS_PER_USER);
            emitter.completeWithError(new IllegalStateException("Per-user SSE connection limit reached"));
            return emitter;
        }

        userEmitters.add(emitter);

        Runnable cleanup = () -> removeConnection(userId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(error -> cleanup.run());

        log.info("SSE connection established for userId={}, active connections={}", userId, getConnectionCount(userId));

        // Event name must match the client's addEventListener('connected', ...)
        send(userId, emitter, () -> SseEmitter.event()
                .name("connected")
                .data("Connected successfully"));

        return emitter;
    }

    /**
     * Fan-out to every connection of one user. The factory is invoked once per
     * recipient because SseEventBuilder.build() consumes the accumulated frame,
     * so one builder instance must not be shared across emitters.
     */
    public int sendToUser(Long userId, Supplier<SseEmitter.SseEventBuilder> eventFactory) {
        List<SseEmitter> emitters = connections.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            log.debug("No active SSE connection for userId={}", userId);
            return 0;
        }

        int sentCount = 0;
        for (SseEmitter emitter : emitters) {
            if (send(userId, emitter, eventFactory)) {
                sentCount++;
            }
        }
        log.debug("Sent SSE event to userId={} on {}/{} connection(s)", userId, sentCount, emitters.size());
        return sentCount;
    }

    /**
     * Sends to one specific connection — used for replay, which must not fan
     * out to the user's other tabs.
     */
    public boolean sendToConnection(Long userId, SseEmitter emitter, Supplier<SseEmitter.SseEventBuilder> eventFactory) {
        return send(userId, emitter, eventFactory);
    }

    public void broadcastToAll(Supplier<SseEmitter.SseEventBuilder> eventFactory) {
        connections.keySet().forEach(userId -> sendToUser(userId, eventFactory));
    }

    public int getConnectionCount(Long userId) {
        List<SseEmitter> emitters = connections.get(userId);
        return emitters != null ? emitters.size() : 0;
    }

    public int getTotalConnections() {
        return connections.values().stream().mapToInt(List::size).sum();
    }

    /**
     * SseEmitter serializes concurrent writes internally, so the heartbeat and
     * Kafka listener threads can target the same emitter safely. Failed sends
     * and already-completed emitters drop the connection instead of throwing.
     */
    private boolean send(Long userId, SseEmitter emitter, Supplier<SseEmitter.SseEventBuilder> eventFactory) {
        try {
            emitter.send(eventFactory.get());
            return true;
        } catch (IOException | IllegalStateException e) {
            log.warn("Failed to send SSE event to userId={}, removing connection: {}", userId, e.getMessage());
            removeConnection(userId, emitter);
            return false;
        }
    }

    private void removeConnection(Long userId, SseEmitter emitter) {
        List<SseEmitter> emitters = connections.get(userId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            connections.remove(userId, emitters);
        }
        log.info("SSE emitter removed for userId={}, remaining connections={}", userId, emitters.size());
    }

    public void closeAllConnections(Long userId) {
        List<SseEmitter> userEmitters = connections.remove(userId);
        if (userEmitters != null) {
            for (SseEmitter emitter : userEmitters) {
                try {
                    emitter.complete();
                } catch (Exception e) {
                    log.warn("Error closing SSE connection for userId={}: {}", userId, e.getMessage());
                }
            }
            log.info("Closed {} SSE connection(s) for userId={}", userEmitters.size(), userId);
        }
    }

    /** Overridable seam so tests can supply emitters with controlled behaviour. */
    protected SseEmitter createEmitter() {
        return new SseEmitter(EMITTER_TIMEOUT_MS);
    }
}
