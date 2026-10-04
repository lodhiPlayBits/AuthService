package com.lodhi.notification_service.websocket;

import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConcurrentSendWebSocketHandlerDecoratorTest {

    private final List<WebSocketSession> established = new CopyOnWriteArrayList<>();
    private final List<WebSocketSession> messagesSeen = new CopyOnWriteArrayList<>();
    private final List<WebSocketSession> closed = new CopyOnWriteArrayList<>();

    private final WebSocketHandler capturingDelegate = new WebSocketHandler() {
        @Override
        public void afterConnectionEstablished(WebSocketSession session) {
            established.add(session);
        }

        @Override
        public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
            messagesSeen.add(session);
        }

        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception) {
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
            closed.add(session);
        }

        @Override
        public boolean supportsPartialMessages() {
            return false;
        }
    };

    @Test
    void allCallbacksForAConnectionReceiveTheSameDecoratedSession() throws Exception {
        WebSocketSession raw = rawSession("session-1");
        WebSocketSessionManager sessionManager = mock(WebSocketSessionManager.class);
        ConcurrentSendWebSocketHandlerDecorator decorator =
                new ConcurrentSendWebSocketHandlerDecorator(capturingDelegate, sessionManager);

        decorator.afterConnectionEstablished(raw);
        decorator.handleMessage(raw, new TextMessage("hello"));
        decorator.afterConnectionClosed(raw, CloseStatus.NORMAL);

        assertEquals(1, established.size());
        WebSocketSession decorated = established.get(0);
        assertNotSame(raw, decorated);
        assertInstanceOf(ConcurrentWebSocketSessionDecorator.class, decorated);
        assertSame(decorated, messagesSeen.get(0));
        assertSame(decorated, closed.get(0));
    }

    @Test
    void concurrentSendsNeverOverlapOnTheUnderlyingSession() throws Exception {
        AtomicInteger inFlight = new AtomicInteger();
        AtomicInteger maxInFlight = new AtomicInteger();
        AtomicInteger delivered = new AtomicInteger();

        WebSocketSession raw = rawSession("session-2");
        doAnswer(invocation -> {
            maxInFlight.accumulateAndGet(inFlight.incrementAndGet(), Math::max);
            try {
                Thread.sleep(20);
            } finally {
                inFlight.decrementAndGet();
            }
            delivered.incrementAndGet();
            return null;
        }).when(raw).sendMessage(any());

        WebSocketSessionManager sessionManager = mock(WebSocketSessionManager.class);
        ConcurrentSendWebSocketHandlerDecorator decorator =
                new ConcurrentSendWebSocketHandlerDecorator(capturingDelegate, sessionManager);
        decorator.afterConnectionEstablished(raw);
        WebSocketSession decorated = established.get(0);

        int threads = 8;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                try {
                    start.await();
                    decorated.sendMessage(new TextMessage("msg"));
                } catch (Exception ignored) {
                } finally {
                    done.countDown();
                }
            }).start();
        }
        start.countDown();

        assertTrue(done.await(10, TimeUnit.SECONDS), "send threads did not finish");
        assertEquals(1, maxInFlight.get(), "underlying session saw overlapping sends");
        assertEquals(threads, delivered.get(), "some messages were lost");
    }

    @Test
    void newConnectionGetsItsOwnDecoratorInstance() throws Exception {
        WebSocketSession first = rawSession("session-a");
        WebSocketSession second = rawSession("session-b");
        WebSocketSessionManager sessionManager = mock(WebSocketSessionManager.class);
        ConcurrentSendWebSocketHandlerDecorator decorator =
                new ConcurrentSendWebSocketHandlerDecorator(capturingDelegate, sessionManager);

        decorator.afterConnectionEstablished(first);
        decorator.handleMessage(first, new TextMessage("x"));
        decorator.afterConnectionClosed(first, CloseStatus.NORMAL);
        decorator.afterConnectionEstablished(second);
        decorator.handleMessage(second, new TextMessage("y"));

        assertNotSame(established.get(0), established.get(1));
        assertSame(established.get(0), messagesSeen.get(0));
        assertSame(established.get(1), messagesSeen.get(1));
    }

    private WebSocketSession rawSession(String id) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(id);
        when(session.isOpen()).thenReturn(true);
        return session;
    }
}
