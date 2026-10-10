package com.lodhi.notification_service.websocket;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class WebSocketSessionManager {

    // Map of userId -> List of active WebSocket sessions
    private final Map<Long, List<WebSocketSession>> userSessions = new ConcurrentHashMap<>();
    
    // Map of sessionId -> WebSocketSession for quick access during expiry check
    private final Map<String, WebSocketSession> allSessions = new ConcurrentHashMap<>();

    public void addSession(WebSocketSession session) {
        if (session.getPrincipal() instanceof StompPrincipal principal) {
            Long userId = principal.userId();
            userSessions.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(session);
            allSessions.put(session.getId(), session);
            log.debug("Added STOMP session {} for userId={}", session.getId(), userId);
        }
    }

    public void removeSession(WebSocketSession session) {
        if (session.getPrincipal() instanceof StompPrincipal principal) {
            Long userId = principal.userId();
            List<WebSocketSession> sessions = userSessions.get(userId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    userSessions.remove(userId);
                }
            }
            allSessions.remove(session.getId());
            log.debug("Removed STOMP session {} for userId={}", session.getId(), userId);
        }
    }

    public void closeSessionsForUser(Long userId) {
        List<WebSocketSession> sessions = userSessions.remove(userId);
        if (sessions != null) {
            for (WebSocketSession session : sessions) {
                closeSessionQuietly(session);
                allSessions.remove(session.getId());
            }
            log.info("Closed {} STOMP session(s) for userId={}", sessions.size(), userId);
        }
    }

    @Scheduled(fixedRate = 60000) // Run every 60 seconds
    public void checkExpirations() {
        Instant now = Instant.now();
        int closedCount = 0;
        
        for (WebSocketSession session : allSessions.values()) {
            if (session.getPrincipal() instanceof StompPrincipal principal 
                    && principal.expiresAt() != null && now.isAfter(principal.expiresAt())) {
                log.info("Closing STOMP session {} for userId={} because JWT token expired", session.getId(), principal.userId());
                closeSessionQuietly(session);
                // Removal from maps is handled by afterConnectionClosed in decorator
                closedCount++;
            }
        }
        
        if (closedCount > 0) {
            log.info("Closed {} expired STOMP sessions during scheduled check", closedCount);
        }
    }

    private void closeSessionQuietly(WebSocketSession session) {
        try {
            session.close();
        } catch (Exception e) {
            log.debug("Error closing STOMP session {}: {}", session.getId(), e.getMessage());
        }
    }
}
