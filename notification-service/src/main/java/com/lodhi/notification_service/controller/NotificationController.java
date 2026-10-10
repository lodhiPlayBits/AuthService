package com.lodhi.notification_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lodhi.notification_service.service.SseConnectionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final SseConnectionManager sseConnectionManager;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventIdHeader,
            @RequestParam(value = "lastEventId", required = false) String lastEventIdParam) {

        Long userId = Long.parseLong(jwt.getSubject());
        SseEmitter emitter = sseConnectionManager.addConnection(userId);

        // EventSource cannot set headers, so a client that reconnects manually
        // also passes its last seen event id as a query parameter.
        String lastEventId = lastEventIdHeader != null ? lastEventIdHeader : lastEventIdParam;
        if (lastEventId != null) {
            replayMissedEvents(userId, emitter, lastEventId);
        }

        return emitter;
    }

    /**
     * Replays events buffered in Redis after {@code lastEventId}. A replay
     * failure only ends the replay — it never kills the fresh stream.
     */
    private void replayMissedEvents(Long userId, SseEmitter emitter, String lastEventId) {
        try {
            List<Object> missedEvents = fetchMissedEvents(userId);
            if (missedEvents == null || missedEvents.isEmpty()) {
                return;
            }

            int replayCount = replayEvents(userId, emitter, lastEventId, missedEvents);
            
            if (replayCount > 0) {
                log.info("Replayed {} missed events for userId={}", replayCount, userId);
            }
        } catch (Exception e) {
            log.warn("Failed to replay missed events for userId={}, keeping stream open: {}", userId, e.getMessage());
        }
    }

    private List<Object> fetchMissedEvents(Long userId) {
        return redisTemplate.opsForList()
                .range(SseConnectionManager.missedEventsKey(userId), 0, -1);
    }

    private int replayEvents(Long userId, SseEmitter emitter, String lastEventId, List<Object> missedEvents) 
            throws Exception {
        boolean startReplay = false;
        int replayCount = 0;
        
        for (Object obj : missedEvents) {
            if (!(obj instanceof Map<?, ?> eventMap)) {
                continue;
            }
            
            String eventId = (String) eventMap.get("eventId");
            
            if (!startReplay) {
                // Everything up to and including lastEventId is old news
                if (lastEventId.equals(eventId)) {
                    startReplay = true;
                }
                continue;
            }
            
            if (!sendEvent(userId, emitter, eventMap, eventId)) {
                return replayCount;
            }
            replayCount++;
        }
        
        return replayCount;
    }

    private boolean sendEvent(Long userId, SseEmitter emitter, Map<?, ?> eventMap, String eventId) 
            throws Exception {
        String eventType = (String) eventMap.get("type");
        String eventData = objectMapper.writeValueAsString(eventMap);
        
        return sseConnectionManager.sendToConnection(userId, emitter, () -> {
            SseEmitter.SseEventBuilder builder = SseEmitter.event()
                    .name(eventType != null ? eventType : "UNKNOWN")
                    .data(eventData)
                    .reconnectTime(5000);
            if (eventId != null) {
                builder.id(eventId);
            }
            return builder;
        });
    }
}
