import { useEffect, useRef, useState, useCallback } from 'react';
import { useAuth } from '../context/AuthContext';

/**
 * Custom hook for subscribing to real-time account notifications via SSE
 * 
 * Features:
 * - Auto-connects when authenticated
 * - Auto-reconnects on connection loss (with exponential backoff)
 * - Handles account disable events -> auto logout
 * - Cleans up on unmount
 */
export const useAccountNotifications = () => {
  const { user, token, isAuthenticated, logout } = useAuth();
  const [connectionState, setConnectionState] = useState('disconnected'); // disconnected, connecting, connected, error
  const [lastEvent, setLastEvent] = useState(null);
  const [notifications, setNotifications] = useState([]);
  
  const eventSourceRef = useRef(null);
  const reconnectTimeoutRef = useRef(null);
  const reconnectAttempts = useRef(0);
  const maxReconnectAttempts = 10;
  const baseReconnectDelay = 1000; // 1 second

  /**
   * Calculate exponential backoff delay
   */
  const getReconnectDelay = useCallback(() => {
    return Math.min(
      baseReconnectDelay * Math.pow(2, reconnectAttempts.current),
      30000 // Max 30 seconds
    );
  }, []);

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

    setConnectionState('connecting');
    console.log('Connecting to SSE stream...');

    const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080';
    // Append userId since notification-service expects it
    const url = `${apiUrl}/api/v1/notifications/stream?userId=${user?.id || user?.userId}`;

    // Use cookie-based authentication with withCredentials
    const eventSource = new EventSource(url, {
      withCredentials: true
    });

    eventSourceRef.current = eventSource;

    // Connected
    eventSource.addEventListener('connected', (e) => {
      console.log('SSE connected:', e.data);
      setConnectionState('connected');
      reconnectAttempts.current = 0;
    });

    // Heartbeat/ping
    eventSource.addEventListener('ping', (e) => {
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
      setConnectionState('connected');
      reconnectAttempts.current = 0;
    };

    // Error handling
    eventSource.onerror = (error) => {
      console.error('SSE error:', error);
      setConnectionState('error');
      
      // Close the current connection
      eventSource.close();
      eventSourceRef.current = null;

      // Attempt reconnection with exponential backoff
      if (reconnectAttempts.current < maxReconnectAttempts) {
        const delay = getReconnectDelay();
        console.log(`Reconnecting in ${delay}ms (attempt ${reconnectAttempts.current + 1}/${maxReconnectAttempts})`);
        
        reconnectTimeoutRef.current = setTimeout(() => {
          reconnectAttempts.current++;
          connect();
        }, delay);
      } else {
        console.error('Max reconnection attempts reached');
        setConnectionState('disconnected');
      }
    };

  }, [user, isAuthenticated, token, getReconnectDelay, handleAccountDisabled, logout, addNotification]);

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

    setConnectionState('disconnected');
    reconnectAttempts.current = 0;
  }, []);

  /**
   * Clear notifications
   */
  const clearNotifications = useCallback(() => {
    setNotifications([]);
    setLastEvent(null);
  }, []);

  // Auto-connect when authenticated
  useEffect(() => {
    if (isAuthenticated) {
      connect();
    } else {
      disconnect();
    }

    // Cleanup on unmount
    return () => {
      disconnect();
    };
  }, [isAuthenticated]); // Only depend on authentication state

  return {
    connectionState,
    lastEvent,
    notifications,
    clearNotifications,
    connect,
    disconnect
  };
};
