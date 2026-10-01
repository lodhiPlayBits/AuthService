package com.lodhi.auth.sse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Sends periodic heartbeat (ping) events to keep SSE connections alive
 * Prevents proxies and load balancers from closing idle connections
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SseHeartbeatScheduler {

    private final SseEmitterRegistry emitterRegistry;

    /**
     * Send heartbeat every 30 seconds
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 30000)
    public void sendHeartbeat() {
        int totalUsers = emitterRegistry.getTotalUsers();
        int totalConnections = emitterRegistry.getTotalConnections();
        
        if (totalConnections > 0) {
            log.debug("Sending SSE heartbeat to {} users ({} connections)", totalUsers, totalConnections);
            
            SseEmitter.SseEventBuilder event = SseEmitter.event()
                    .name("ping")
                    .data("heartbeat");
            
            emitterRegistry.broadcastToAll(event);
        }
    }
}
