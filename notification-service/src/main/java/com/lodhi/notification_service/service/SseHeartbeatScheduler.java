package com.lodhi.notification_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Sends periodic ping events so proxies and load balancers don't close idle
 * SSE connections; the client listens for the "ping" event.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SseHeartbeatScheduler {

    private final SseConnectionManager sseConnectionManager;

    @Scheduled(fixedDelay = 30000, initialDelay = 30000)
    public void sendHeartbeat() {
        int totalConnections = sseConnectionManager.getTotalConnections();
        if (totalConnections > 0) {
            log.debug("Sending SSE heartbeat to {} connection(s)", totalConnections);
            sseConnectionManager.broadcastToAll(() -> SseEmitter.event()
                    .name("ping")
                    .data("heartbeat"));
        }
    }
}
