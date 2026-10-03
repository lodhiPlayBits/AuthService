package com.lodhi.notification_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lodhi.notification.contract.events.AccountEvent;
import com.lodhi.notification_service.service.AccountEventConsumer;
import com.lodhi.notification_service.service.SseConnectionManager;

@ExtendWith(MockitoExtension.class)
class AccountEventConsumerTest {

    private static final String MISSED_EVENTS_KEY = "notification:v1:sse:missed:7";

    @Mock
    private SseConnectionManager sseConnectionManager;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ListOperations<String, Object> listOperations;

    @Captor
    private ArgumentCaptor<Supplier<SseEmitter.SseEventBuilder>> eventFactoryCaptor;

    private AccountEventConsumer consumer;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        consumer = new AccountEventConsumer(sseConnectionManager, redisTemplate, objectMapper);
    }

    @Test
    void pushesEventFrameAndBuffersForReplay() {
        when(sseConnectionManager.sendToUser(eq(7L), any())).thenReturn(1);
        when(redisTemplate.opsForList()).thenReturn(listOperations);

        AccountEvent accountEvent = event("evt-1");
        consumer.handleAccountEvent(accountEvent);

        verify(sseConnectionManager).sendToUser(eq(7L), eventFactoryCaptor.capture());
        assertThat(SseEventFrame.render(eventFactoryCaptor.getValue().get()))
                .contains("id:evt-1", "event:ACCOUNT_DISABLED", "retry:5000",
                        "\"eventId\":\"evt-1\"", "Account disabled by admin");

        verify(listOperations).rightPush(MISSED_EVENTS_KEY, accountEvent);
        verify(listOperations).trim(MISSED_EVENTS_KEY, -50, -1);
        verify(redisTemplate).expire(MISSED_EVENTS_KEY, Duration.ofDays(7));
    }

    @Test
    void buffersEvenWhenNoOneConnectedAndOmitsIdFrame() {
        when(sseConnectionManager.sendToUser(eq(7L), any())).thenReturn(0);
        when(redisTemplate.opsForList()).thenReturn(listOperations);

        AccountEvent accountEvent = event(null);
        consumer.handleAccountEvent(accountEvent);

        verify(sseConnectionManager).sendToUser(eq(7L), eventFactoryCaptor.capture());
        assertThat(SseEventFrame.render(eventFactoryCaptor.getValue().get()))
                .contains("event:ACCOUNT_DISABLED")
                .doesNotContain("id:");

        verify(listOperations).rightPush(MISSED_EVENTS_KEY, accountEvent);
    }

    @Test
    void serializationFailurePropagatesToKafkaErrorHandler() {
        AccountEvent broken = AccountEvent.builder()
                .eventId("evt-broken")
                .type(AccountEvent.AccountEventType.ROLE_CHANGED)
                .userId(7L)
                .payload(new Unserializable())
                .timestamp(Instant.parse("2026-01-01T00:00:00Z"))
                .build();

        assertThatThrownBy(() -> consumer.handleAccountEvent(broken))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("evt-broken");

        verifyNoInteractions(sseConnectionManager, redisTemplate);
    }

    private AccountEvent event(String eventId) {
        return AccountEvent.builder()
                .eventId(eventId)
                .type(AccountEvent.AccountEventType.ACCOUNT_DISABLED)
                .userId(7L)
                .email("user@example.com")
                .message("Account disabled by admin")
                .timestamp(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }

    static class Unserializable {

        public String getExplode() {
            throw new IllegalStateException("cannot serialize");
        }
    }
}
