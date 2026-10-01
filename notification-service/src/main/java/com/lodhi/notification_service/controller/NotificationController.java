package com.lodhi.notification_service.controller;

import com.lodhi.notification_service.service.SseConnectionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final SseConnectionManager sseConnectionManager;

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam("userId") Long userId) {
        // In a real application, extract userId from JWT token
        // e.g. using @AuthenticationPrincipal
        return sseConnectionManager.addConnection(userId);
    }
}
