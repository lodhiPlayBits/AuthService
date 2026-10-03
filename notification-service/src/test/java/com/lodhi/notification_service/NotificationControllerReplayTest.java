package com.lodhi.notification_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lodhi.notification_service.controller.NotificationController;
import com.lodhi.notification_service.service.SseConnectionManager;

@ExtendWith(MockitoExtension.class)
class NotificationControllerReplayTest {

    private static final String MISSED_EVENTS_KEY = "notification:v1:sse:missed:42";

    @Mock
    private SseConnectionManager sseConnectionManager;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ListOperations<String, Object> listOperations;

    @Mock
    private SseEmitter emitter;

    @Captor
    private ArgumentCaptor<Supplier<SseEmitter.SseEventBuilder>> eventFactoryCaptor;

    private NotificationController controller;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        controller = new NotificationController(sseConnectionManager, redisTemplate, new ObjectMapper());
        jwt = Jwt.withTokenValue("test-token").header("alg", "none").subject("42").build();
    }

    @Test
    void replaysBufferedEventsAfterLastEventIdHeader() {
        when(sseConnectionManager.addConnection(42L)).thenReturn(emitter);
        when(sseConnectionManager.sendToConnection(eq(42L), eq(emitter), any())).thenReturn(true);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.range(MISSED_EVENTS_KEY, 0, -1)).thenReturn(List.<Object>of(
                event("e1", "ACCOUNT_DISABLED"), event("e2", "ROLE_CHANGED"), event("e3", "PASSWORD_CHANGED")));

        SseEmitter result = controller.stream(jwt, "e1", null);

        assertThat(result).isSameAs(emitter);
        verify(sseConnectionManager, times(2)).sendToConnection(eq(42L), eq(emitter), eventFactoryCaptor.capture());
        List<Supplier<SseEmitter.SseEventBuilder>> factories = eventFactoryCaptor.getAllValues();

        assertThat(SseEventFrame.render(factories.get(0).get()))
                .contains("id:e2", "event:ROLE_CHANGED", "retry:5000", "\"eventId\":\"e2\"");
        assertThat(SseEventFrame.render(factories.get(1).get()))
                .contains("id:e3", "event:PASSWORD_CHANGED");
    }

    @Test
    void replaysWhenLastEventIdComesFromQueryParam() {
        when(sseConnectionManager.addConnection(42L)).thenReturn(emitter);
        when(sseConnectionManager.sendToConnection(eq(42L), eq(emitter), any())).thenReturn(true);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.range(MISSED_EVENTS_KEY, 0, -1)).thenReturn(List.<Object>of(
                event("e1", "ACCOUNT_DISABLED"), event("e2", "ROLE_CHANGED")));

        SseEmitter result = controller.stream(jwt, null, "e1");

        assertThat(result).isSameAs(emitter);
        verify(sseConnectionManager).sendToConnection(eq(42L), eq(emitter), eventFactoryCaptor.capture());
        assertThat(SseEventFrame.render(eventFactoryCaptor.getValue().get()))
                .contains("id:e2", "event:ROLE_CHANGED");
    }

    @Test
    void doesNotTouchRedisWithoutLastEventId() {
        when(sseConnectionManager.addConnection(42L)).thenReturn(emitter);

        SseEmitter result = controller.stream(jwt, null, null);

        assertThat(result).isSameAs(emitter);
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void replaysNothingWhenLastEventIdIsLatest() {
        when(sseConnectionManager.addConnection(42L)).thenReturn(emitter);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.range(MISSED_EVENTS_KEY, 0, -1)).thenReturn(List.<Object>of(
                event("e1", "ACCOUNT_DISABLED"), event("e2", "ACCOUNT_ENABLED"), event("e3", "ROLE_CHANGED")));

        controller.stream(jwt, "e3", null);

        verify(sseConnectionManager, never()).sendToConnection(any(), any(), any());
    }

    @Test
    void replayFailureKeepsStreamOpen() {
        when(sseConnectionManager.addConnection(42L)).thenReturn(emitter);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.range(MISSED_EVENTS_KEY, 0, -1))
                .thenThrow(new DataAccessResourceFailureException("redis down"));

        SseEmitter result = controller.stream(jwt, "e1", null);

        assertThat(result).isSameAs(emitter);
        verify(sseConnectionManager, never()).sendToConnection(any(), any(), any());
    }

    @Test
    void stopsReplayWhenConnectionSendFails() {
        when(sseConnectionManager.addConnection(42L)).thenReturn(emitter);
        when(sseConnectionManager.sendToConnection(eq(42L), eq(emitter), any())).thenReturn(true, false);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.range(MISSED_EVENTS_KEY, 0, -1)).thenReturn(List.<Object>of(
                event("e1", "ACCOUNT_DISABLED"), event("e2", "ROLE_CHANGED"), event("e3", "PASSWORD_CHANGED")));

        controller.stream(jwt, "e1", null);

        verify(sseConnectionManager, times(2)).sendToConnection(eq(42L), eq(emitter), any());
    }

    @Test
    void skipsNonMapEntriesInBuffer() {
        when(sseConnectionManager.addConnection(42L)).thenReturn(emitter);
        when(sseConnectionManager.sendToConnection(eq(42L), eq(emitter), any())).thenReturn(true);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.range(MISSED_EVENTS_KEY, 0, -1)).thenReturn(List.<Object>of(
                "corrupted-entry", event("e1", "ACCOUNT_DISABLED"), event("e2", "ROLE_CHANGED")));

        controller.stream(jwt, "e1", null);

        verify(sseConnectionManager).sendToConnection(eq(42L), eq(emitter), eventFactoryCaptor.capture());
        assertThat(SseEventFrame.render(eventFactoryCaptor.getValue().get()))
                .contains("id:e2", "event:ROLE_CHANGED");
    }

    private Map<String, Object> event(String eventId, String type) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("eventId", eventId);
        event.put("type", type);
        event.put("userId", 42);
        event.put("message", "message-" + eventId);
        return event;
    }
}
