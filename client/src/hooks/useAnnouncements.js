import { useCallback, useEffect, useRef, useState } from 'react';
import { Client, ReconnectionTimeMode } from '@stomp/stompjs';
import { useAuth } from '../context/AuthContext';
import { getReconnectDelay, RECONNECT_MAX_DELAY_MS } from '../utils/backoff';

/**
 * Custom hook for the real-time announcement channel over STOMP (WebSocket).
 *
 * Features:
 * - Auto-connects when authenticated (JWT passed as a query param on the
 *   handshake, matching JwtHandshakeInterceptor on the server)
 * - Auto-reconnects via @stomp/stompjs with truncated exponential backoff
 *   (jittered start, doubling per failed attempt, 30s cap, reset on every
 *   successful CONNECT; subscriptions are re-created on each (re)connect
 *   because they live inside onConnect)
 * - Recreates the connection when the access token rotates
 * - Subscribes /topic/announcements + /user/queue/** and,
 *   for admins, /topic/admin.responses
 *
 * SSE (account events such as ACCOUNT_DISABLED) stays on EventSource in
 * useAccountNotifications — stompjs only speaks STOMP over WebSocket.
 */

const MAX_ITEMS = 20;

const buildBrokerUrl = (token) => {
  const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080';
  const wsBase = apiUrl.replace(/^http/, 'ws');
  return `${wsBase}/ws/announcements?token=${encodeURIComponent(token)}`;
};

const parseJson = (body) => {
  try {
    return JSON.parse(body);
  } catch (error) {
    console.error('Failed to parse STOMP message body:', error);
    return null;
  }
};

export const useAnnouncements = () => {
  const { user, token, isAuthenticated } = useAuth();
  const [socketStatus, setSocketStatus] = useState('idle'); // idle | connected | error
  const [announcements, setAnnouncements] = useState([]);
  const [confirmations, setConfirmations] = useState([]);
  const [adminResponses, setAdminResponses] = useState([]);
  const [errors, setErrors] = useState([]);

  const clientRef = useRef(null);
  // Kept in a ref so onConnect always reads the current value; a role change
  // alone must not tear down a healthy connection.
  const isAdminRef = useRef(false);

  useEffect(() => {
    isAdminRef.current = user?.role === 'ADMIN';
  }, [user]);

  // Derived so session changes never require a setState from an effect:
  // no session means disconnected, otherwise idle means a connect is underway.
  const connectionState = !isAuthenticated || !token
    ? 'disconnected'
    : socketStatus === 'idle'
      ? 'connecting'
      : socketStatus;

  useEffect(() => {
    if (!isAuthenticated || !token) return undefined;

    const client = new Client({
      brokerURL: buildBrokerUrl(token),
      // stompjs drives the retry loop itself: EXPONENTIAL doubles the delay
      // after each failed attempt and resets it on a successful CONNECT.
      // Only the initial delay is ours — getReconnectDelay(0) makes it a
      // jittered 0.5–1s so a fleet of clients that dropped together (server
      // restart) doesn't re-handshake in lockstep and trip nginx's limit.
      reconnectDelay: getReconnectDelay(0),
      maxReconnectDelay: RECONNECT_MAX_DELAY_MS,
      reconnectTimeMode: ReconnectionTimeMode.EXPONENTIAL,
      // A hung TCP/upgrade handshake never fires a close event, which would
      // stall the retry loop in "connecting" forever; time it out instead.
      connectionTimeout: 10000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        if (clientRef.current !== client) return;
        setSocketStatus('connected');

        client.subscribe('/topic/announcements', (message) => {
          console.log('STOMP /topic/announcements received:', message.body);
          const announcement = parseJson(message.body);
          if (announcement) {
            // Only show announcements sent by OTHER users
            if (announcement.sentBy !== user?.id) {
              announcement.type = 'ANNOUNCEMENT';
              setAnnouncements((prev) => [announcement, ...prev].slice(0, MAX_ITEMS));
            } else {
              console.log('Ignoring self-sent announcement:', announcement.title);
            }
          }
        });

        client.subscribe('/user/queue/confirmations', (message) => {
          console.log('STOMP /user/queue/confirmations received:', message.body);
          const confirmation = parseJson(message.body);
          if (confirmation) {
            setConfirmations((prev) => [confirmation, ...prev].slice(0, MAX_ITEMS));
          }
        });

        client.subscribe('/user/queue/errors', (message) => {
          console.error('STOMP /user/queue/errors received:', message.body);
          const error = parseJson(message.body);
          if (error) {
            setErrors((prev) => [error, ...prev].slice(0, MAX_ITEMS));
          }
        });

        if (isAdminRef.current) {
          client.subscribe('/topic/admin.responses', (message) => {
            console.log('STOMP /topic/admin.responses received:', message.body);
            const response = parseJson(message.body);
            if (response) {
              setAdminResponses((prev) => [response, ...prev].slice(0, MAX_ITEMS));
            }
          });
        }
      },
      onWebSocketClose: () => {
        // Ignore close events from a client that was already replaced/removed.
        if (clientRef.current !== client) return;
        setSocketStatus('idle');
      },
      onStompError: (frame) => {
        console.error('STOMP broker error:', frame.headers?.message, frame.body);
        if (clientRef.current === client) setSocketStatus('error');
      },
      onWebSocketError: (event) => {
        console.error('WebSocket error:', event);
        if (clientRef.current === client) setSocketStatus('error');
      },
    });

    clientRef.current = client;
    client.activate();

    return () => {
      clientRef.current = null;
      setSocketStatus('idle');
      client.deactivate();
    };
  }, [isAuthenticated, token]);

  const publish = useCallback((destination, body) => {
    const client = clientRef.current;
    if (!client?.connected) {
      console.warn(`Cannot publish to ${destination}: STOMP connection is not active`);
      return false;
    }
    client.publish({ destination, body: JSON.stringify(body) });
    return true;
  }, []);

  const sendAnnouncement = useCallback(
    (title, message, priority) => publish('/app/announcement.send', { title, message, priority }),
    [publish]
  );

  const acknowledgeAnnouncement = useCallback(
    (announcementId) => publish('/app/announcement.ack', { announcementId }),
    [publish]
  );

  const dismissAnnouncement = useCallback(
    (announcementId) => publish('/app/announcement.dismiss', { announcementId }),
    [publish]
  );

  return {
    connectionState,
    announcements,
    confirmations,
    adminResponses,
    errors,
    sendAnnouncement,
    acknowledgeAnnouncement,
    dismissAnnouncement,
  };
};
