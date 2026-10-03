package com.lodhi.notification_service.websocket;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gives every WebSocket connection a single {@link ConcurrentWebSocketSessionDecorator} that
 * serializes sends per session, so the heartbeat scheduler, the Kafka announcement consumer,
 * and request-handling threads can never write to the same underlying session concurrently.
 *
 * <p>The wrapper instance is cached per connection ID and handed to every handler callback,
 * because {@link ConcurrentWebSocketSessionDecorator} only guarantees ordering for sends
 * issued through the same decorator instance.
 */
public class ConcurrentSendWebSocketHandlerDecorator extends WebSocketHandlerDecorator {

    private static final int SEND_TIME_LIMIT_MS = 10_000;
    private static final int BUFFER_SIZE_BYTES = 256 * 1024;

    private final Map<String, WebSocketSession> decoratedSessions = new ConcurrentHashMap<>();

    public ConcurrentSendWebSocketHandlerDecorator(WebSocketHandler delegate) {
        super(delegate);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        WebSocketSession decorated = new ConcurrentWebSocketSessionDecorator(
                session, SEND_TIME_LIMIT_MS, BUFFER_SIZE_BYTES);
        decoratedSessions.put(session.getId(), decorated);
        getDelegate().afterConnectionEstablished(decorated);
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        getDelegate().handleMessage(decorated(session), message);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        getDelegate().handleTransportError(decorated(session), exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        WebSocketSession decorated = decoratedSessions.remove(session.getId());
        getDelegate().afterConnectionClosed(decorated != null ? decorated : session, closeStatus);
    }

    private WebSocketSession decorated(WebSocketSession session) {
        return decoratedSessions.getOrDefault(session.getId(), session);
    }
}
