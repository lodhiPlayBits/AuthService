package com.lodhi.notification_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.lodhi.notification_service.service.SseConnectionManager;

class SseConnectionManagerTest {

    private Deque<SseEmitter> emitterQueue;
    private TestableConnectionManager manager;

    @BeforeEach
    void setUp() {
        emitterQueue = new ArrayDeque<>();
        manager = new TestableConnectionManager(emitterQueue);
    }

    @Test
    void addConnectionSendsLowerCaseConnectedEvent() throws Exception {
        SseEmitter emitter = enqueueSpyEmitter();

        manager.addConnection(1L);

        ArgumentCaptor<SseEmitter.SseEventBuilder> captor = ArgumentCaptor.forClass(SseEmitter.SseEventBuilder.class);
        verify(emitter).send(captor.capture());
        assertThat(SseEventFrame.render(captor.getValue()))
                .contains("event:connected", "data:Connected successfully");
    }

    @Test
    void secondConnectionDoesNotOrphanFirstEmitter() throws Exception {
        SseEmitter first = enqueueSpyEmitter();
        SseEmitter second = enqueueSpyEmitter();
        manager.addConnection(1L);
        manager.addConnection(1L);

        int sent = manager.sendToUser(1L, () -> SseEmitter.event().name("ACCOUNT_ENABLED").data("{}"));

        assertThat(sent).isEqualTo(2);
        assertThat(manager.getConnectionCount(1L)).isEqualTo(2);

        ArgumentCaptor<SseEmitter.SseEventBuilder> firstCaptor = ArgumentCaptor.forClass(SseEmitter.SseEventBuilder.class);
        ArgumentCaptor<SseEmitter.SseEventBuilder> secondCaptor = ArgumentCaptor.forClass(SseEmitter.SseEventBuilder.class);
        verify(first, times(2)).send(firstCaptor.capture());
        verify(second, times(2)).send(secondCaptor.capture());

        // Each recipient must get its own intact frame (no shared, consumed builder)
        String firstFrame = SseEventFrame.render(firstCaptor.getAllValues().get(1));
        String secondFrame = SseEventFrame.render(secondCaptor.getAllValues().get(1));
        assertThat(firstFrame)
                .contains("event:ACCOUNT_ENABLED")
                .isEqualTo(secondFrame);
    }

    @Test
    void sendToUserWithoutConnectionsReturnsZero() {
        assertThat(manager.sendToUser(99L, () -> SseEmitter.event().name("X").data("{}"))).isZero();
    }

    @Test
    void rejectsSixthConnectionForSameUser() {
        for (int i = 0; i < 5; i++) {
            enqueueRealEmitter();
            manager.addConnection(1L);
        }
        SseEmitter rejected = enqueueSpyEmitter();

        SseEmitter returned = manager.addConnection(1L);

        assertThat(returned).isSameAs(rejected);
        verify(rejected).completeWithError(any(IllegalStateException.class));
        assertThat(manager.getConnectionCount(1L)).isEqualTo(5);
    }

    @Test
    void rejectsConnectionWhenGlobalLimitReached() {
        // SseConnectionManager.MAX_TOTAL_CONNECTIONS is 1000 => 200 users x 5 connections
        for (long userId = 0; userId < 200; userId++) {
            for (int i = 0; i < 5; i++) {
                enqueueRealEmitter();
                manager.addConnection(userId);
            }
        }
        SseEmitter rejected = enqueueSpyEmitter();

        SseEmitter returned = manager.addConnection(999L);

        assertThat(returned).isSameAs(rejected);
        verify(rejected).completeWithError(any(IllegalStateException.class));
        assertThat(manager.getTotalConnections()).isEqualTo(1000);
    }

    @Test
    void ioExceptionDuringSendDropsOnlyBrokenConnection() throws Exception {
        SseEmitter broken = enqueueSpyEmitter();
        enqueueRealEmitter();
        manager.addConnection(1L);
        manager.addConnection(1L);

        doThrow(new IOException("broken pipe")).when(broken).send(any(SseEmitter.SseEventBuilder.class));

        int sent = manager.sendToUser(1L, () -> SseEmitter.event().name("ACCOUNT_ENABLED").data("{}"));

        assertThat(sent).isEqualTo(1);
        assertThat(manager.getConnectionCount(1L)).isEqualTo(1);
    }

    @Test
    void completedEmitterDoesNotThrowIntoCaller() {
        SseEmitter completed = enqueueRealEmitter();
        manager.addConnection(1L);
        completed.completeWithError(new IOException("client gone"));

        int sent = manager.sendToUser(1L, () -> SseEmitter.event().name("ACCOUNT_ENABLED").data("{}"));

        assertThat(sent).isZero();
        assertThat(manager.getConnectionCount(1L)).isZero();
    }

    @Test
    void completionCallbackRemovesOnlyThatEmitter() {
        SseEmitter first = enqueueSpyEmitter();
        SseEmitter second = enqueueSpyEmitter();
        manager.addConnection(1L);
        manager.addConnection(1L);

        ArgumentCaptor<Runnable> firstCleanup = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Runnable> secondCleanup = ArgumentCaptor.forClass(Runnable.class);
        verify(first).onCompletion(firstCleanup.capture());
        verify(second).onCompletion(secondCleanup.capture());

        firstCleanup.getValue().run();
        assertThat(manager.getConnectionCount(1L)).isEqualTo(1);

        firstCleanup.getValue().run();
        assertThat(manager.getConnectionCount(1L)).isEqualTo(1);

        secondCleanup.getValue().run();
        assertThat(manager.getConnectionCount(1L)).isZero();
    }

    @Test
    void broadcastReachesEveryUser() throws Exception {
        SseEmitter userA = enqueueSpyEmitter();
        SseEmitter userB = enqueueSpyEmitter();
        manager.addConnection(1L);
        manager.addConnection(2L);

        manager.broadcastToAll(() -> SseEmitter.event().name("ping").data("heartbeat"));

        verify(userA, times(2)).send(any(SseEmitter.SseEventBuilder.class));
        verify(userB, times(2)).send(any(SseEmitter.SseEventBuilder.class));
    }

    private SseEmitter enqueueRealEmitter() {
        SseEmitter emitter = new SseEmitter();
        emitterQueue.add(emitter);
        return emitter;
    }

    private SseEmitter enqueueSpyEmitter() {
        SseEmitter emitter = spy(new SseEmitter());
        emitterQueue.add(emitter);
        return emitter;
    }

    static class TestableConnectionManager extends SseConnectionManager {

        private final Deque<SseEmitter> emitters;

        TestableConnectionManager(Deque<SseEmitter> emitters) {
            this.emitters = emitters;
        }

        @Override
        protected SseEmitter createEmitter() {
            SseEmitter next = emitters.poll();
            return next != null ? next : new SseEmitter();
        }
    }
}
