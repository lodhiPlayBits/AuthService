import { useEffect, useRef, useState, useCallback } from 'react';
import { useAuth } from '../context/AuthContext';
import { getReconnectDelay } from '../utils/backoff';

/**
 * Custom hook for subscribing to real-time account notifications via SSE
 * 
 * Features:
 * - Auto-connects when authenticated
 * - Auto-reconnects on connection loss with jittered exponential backoff
 *   (capped at 30s; retries indefinitely — a recovered server may be
 *   reachable minutes later, and giving up would leave the user silently
 *   offline until a page reload)
 * - Handles account disable events -> auto logout
 * - Cleans up on unmount
 */
export const useAccountNotifications = () => {
  const { token, isAuthenticated, logout } = useAuth();
  const [streamStatus, setStreamStatus] = useState('idle'); // idle | connecting | connected | error
  const [lastEvent, setLastEvent] = useState(null);
  const [notifications, setNotifications] = useState([]);
  
  const eventSourceRef = useRef(null);
  const lastEventIdRef = useRef(null);
  const reconnectTimeoutRef = useRef(null);
  const reconnectAttempts = useRef(0);
  const connectRef = useRef(null);
  const disconnectRef = useRef(null);

  // Derived so session changes never require a setState from an effect:
  // no session means disconnected, otherwise idle means a connect is underway.
  const connectionState = !isAuthenticated || !token
    ? 'disconnected'
    : streamStatus === 'idle'
      ? 'connecting'
      : streamStatus;

  /**
   * Add notification to the list (keeping last 10)
   */
  const addNotification = useCallback((event) => {
    const notification = {
      id: event.eventId || Date.now().toString(),
      type: event.type,
      message: event.message,
      timestamp: event.timestamp || Date.now(),
      data: event
    };
    
    setNotifications(prev => [notification, ...prev].slice(0, 10));
    setLastEvent(notification);
  }, []);

  /**
   * Handle account disabled event - logout user
   */
  const handleAccountDisabled = useCallback((event) => {
    console.warn('Account disabled event received:', event);
    
    addNotification({
      ...event,
      message: event.message || 'Your account has been disabled'
    });

    // Close SSE connection
    if (eventSourceRef.current) {
      eventSourceRef.current.close();
      eventSourceRef.current = null;
    }

    // Logout user and redirect
    setTimeout(() => {
      logout();
      alert('Your account has been disabled. Please contact support.');
      window.location.href = '/client/login';
    }, 1000);
  }, [logout, addNotification]);

  /**
   * Connect to SSE stream
   */
  const connect = useCallback(() => {
    if (!isAuthenticated || !token) {
      console.log('Not authenticated, skipping SSE connection');
      return;
    }

    if (eventSourceRef.current) {
      console.log('SSE connection already exists');
      return;
    }

    console.log('Connecting to SSE stream...');

    const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080';
    // EventSource cannot set headers, so the JWT is passed as a query parameter;
    // notification-service only accepts it on this exact stream path.
    let url = `${apiUrl}/api/v1/notifications/stream?token=${encodeURIComponent(token)}`;
    // Manual reconnects create a new EventSource, which loses lastEventId;
    // carry it explicitly so the server can replay events missed while offline.
    if (lastEventIdRef.current) {
      url += `&lastEventId=${encodeURIComponent(lastEventIdRef.current)}`;
    }

    const eventSource = new EventSource(url);

    eventSourceRef.current = eventSource;

    // Connected
    eventSource.addEventListener('connected', (e) => {
      console.log('SSE connected:', e.data);
      setStreamStatus('connected');
      reconnectAttempts.current = 0;
    });

    // Heartbeat/ping
    eventSource.addEventListener('ping', () => {
      console.debug('SSE heartbeat received');
    });

    // Account disabled event
    eventSource.addEventListener('ACCOUNT_DISABLED', (e) => {
      try {
        const event = JSON.parse(e.data);
        handleAccountDisabled(event);
      } catch (error) {
        console.error('Failed to parse ACCOUNT_DISABLED event:', error);
      }
    });

    // Account enabled event
    eventSource.addEventListener('ACCOUNT_ENABLED', (e) => {
      try {
        const event = JSON.parse(e.data);
        console.log('Account enabled event:', event);
        addNotification(event);
      } catch (error) {
        console.error('Failed to parse ACCOUNT_ENABLED event:', error);
      }
    });

    // Session revoked event
    eventSource.addEventListener('SESSION_REVOKED', (e) => {
      try {
        const event = JSON.parse(e.data);
        console.warn('Session revoked event:', event);
        addNotification(event);
        
        // Logout after a brief delay
        setTimeout(() => {
          logout();
          alert('Your session has been revoked.');
          window.location.href = '/client/login';
        }, 1000);
      } catch (error) {
        console.error('Failed to parse SESSION_REVOKED event:', error);
      }
    });

    // Connection opened
    eventSource.onopen = () => {
      console.log('SSE connection opened');
      setStreamStatus('connected');
      reconnectAttempts.current = 0;
    };

    // Error handling
    eventSource.onerror = (error) => {
      console.error('SSE error:', error);
      setStreamStatus('error');

      // Preserve the last seen event id before dropping this EventSource
      lastEventIdRef.current = eventSource.lastEventId || lastEventIdRef.current;
      
      // Close the current connection
      eventSource.close();
      eventSourceRef.current = null;

      // Jittered exponential backoff, capped at 30s, retried indefinitely.
      // The counter resets on every successful connection (below), so this
      // only ever slows down an unhealthy stream.
      const attempt = reconnectAttempts.current;
      reconnectAttempts.current = attempt + 1;
      const delay = getReconnectDelay(attempt);
      console.log(`SSE reconnecting in ${delay}ms (attempt ${attempt + 1})`);

      reconnectTimeoutRef.current = setTimeout(() => {
        setStreamStatus('connecting');
        connectRef.current?.();
      }, delay);
    };

  }, [isAuthenticated, token, handleAccountDisabled, addNotification, logout]);

  /**
   * Disconnect from SSE stream
   */
  const disconnect = useCallback(() => {
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current);
      reconnectTimeoutRef.current = null;
    }

    if (eventSourceRef.current) {
      console.log('Closing SSE connection');
      eventSourceRef.current.close();
      eventSourceRef.current = null;
    }

    setStreamStatus('idle');
    reconnectAttempts.current = 0;
    // lastEventIdRef is intentionally kept: reconnects after token rotation
    // must still replay events missed during the switch. It is reset on logout.
  }, []);

  /**
   * Clear notifications
   */
  const clearNotifications = useCallback(() => {
    setNotifications([]);
    setLastEvent(null);
  }, []);

  // Keep the latest callbacks in refs so the auto-connect effect can call them
  // without re-running on every identity change, and so connect() can
  // reschedule itself without a self-reference.
  useEffect(() => {
    connectRef.current = connect;
    disconnectRef.current = disconnect;
  });

  // Auto-connect when authenticated. The token is an effect dependency so a
  // rotated (refreshed) access token reconnects the SSE stream.
  useEffect(() => {
    if (isAuthenticated && token) {
      connectRef.current?.();
    } else {
      // The previous effect's cleanup already closed any open stream; a fresh
      // session must not replay events from the old one.
      lastEventIdRef.current = null;
    }

    // Cleanup on unmount
    return () => {
      disconnectRef.current?.();
    };
  }, [isAuthenticated, token]);

  return {
    connectionState,
    lastEvent,
    notifications,
    clearNotifications
  };
};
