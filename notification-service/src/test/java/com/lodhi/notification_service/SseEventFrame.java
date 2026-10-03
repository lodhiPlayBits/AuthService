package com.lodhi.notification_service;

import java.util.stream.Collectors;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public final class SseEventFrame {

    private SseEventFrame() {
    }

    public static String render(SseEmitter.SseEventBuilder builder) {
        return builder.build().stream()
                .map(part -> String.valueOf(part.getData()))
                .collect(Collectors.joining());
    }
}
