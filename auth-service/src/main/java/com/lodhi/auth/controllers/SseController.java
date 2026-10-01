package com.lodhi.auth.controllers;

import com.lodhi.auth.security.JwtPrincipal;
import com.lodhi.auth.sse.SseEmitterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * Controller for Server-Sent Events (SSE) streaming
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class SseController {

    private final SseEmitterRegistry emitterRegistry;

    /**
     * SSE endpoint for real-time notifications
     * Client connects and receives events as they occur
     * Authentication via JWT cookie (refreshToken or session cookie)
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNotifications(@AuthenticationPrincipal JwtPrincipal principal) {
        Long userId = principal.getUserId();
        log.info("SSE connection requested for userId={}", userId);

        // Create emitter with no timeout (0 = infinite)
        // Heartbeat scheduler keeps connection alive
        SseEmitter emitter = new SseEmitter(0L);

        // Register the emitter
        emitterRegistry.addEmitter(userId, emitter);

        // Send initial connection event
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("Connected to notification stream"));
            log.info("SSE connection established for userId={}", userId);
        } catch (IOException e) {
            log.error("Failed to send connection event to userId={}", userId, e);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    /**
     * Health check endpoint to see SSE stats
     */
    @GetMapping("/stats")
    public SseStatsResponse getStats() {
        return new SseStatsResponse(
            emitterRegistry.getTotalUsers(),
            emitterRegistry.getTotalConnections()
        );
    }

    public record SseStatsResponse(int totalUsers, int totalConnections) {}
}
