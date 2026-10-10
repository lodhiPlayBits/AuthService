package com.lodhi.notification_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.lodhi.notification_service.service.SseConnectionManager;
import com.lodhi.notification_service.service.SseHeartbeatScheduler;

@ExtendWith(MockitoExtension.class)
class SseHeartbeatSchedulerTest {

    @Mock
    private SseConnectionManager sseConnectionManager;

    @Captor
    private ArgumentCaptor<Supplier<SseEmitter.SseEventBuilder>> eventFactoryCaptor;

    private SseHeartbeatScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new SseHeartbeatScheduler(sseConnectionManager);
    }

    @Test
    void doesNothingWhenNoConnections() {
        when(sseConnectionManager.getTotalConnections()).thenReturn(0);

        scheduler.sendHeartbeat();

        verify(sseConnectionManager, never()).broadcastToAll(any());
    }

    @Test
    void broadcastsPingFrameWhenConnectionsExist() {
        when(sseConnectionManager.getTotalConnections()).thenReturn(2);

        scheduler.sendHeartbeat();

        verify(sseConnectionManager).broadcastToAll(eventFactoryCaptor.capture());
        assertThat(SseEventFrame.render(eventFactoryCaptor.getValue().get()))
                .contains("event:ping", "data:heartbeat");
    }
}
