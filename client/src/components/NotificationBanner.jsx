import { useEffect, useState } from 'react';
import './NotificationBanner.css';

/**
 * Notification banner component to display real-time notifications
 */
const NotificationBanner = ({ notification, onClose }) => {
  const [isVisible, setIsVisible] = useState(false);

  useEffect(() => {
    if (notification) {
      setIsVisible(true);
      
      // Auto-dismiss after 5 seconds (except for critical events)
      const criticalEvents = ['ACCOUNT_DISABLED', 'SESSION_REVOKED'];
      if (!criticalEvents.includes(notification.type)) {
        const timer = setTimeout(() => {
          handleClose();
        }, 5000);
        
        return () => clearTimeout(timer);
      }
    }
  }, [notification]);

  const handleClose = () => {
    setIsVisible(false);
    setTimeout(() => {
      onClose?.();
    }, 300); // Wait for animation to complete
  };

  if (!notification) return null;

  const getNotificationClass = () => {
    switch (notification.type) {
      case 'ACCOUNT_DISABLED':
      case 'SESSION_REVOKED':
        return 'notification-error';
      case 'ACCOUNT_ENABLED':
        return 'notification-success';
      default:
        return 'notification-info';
    }
  };

  const getIcon = () => {
    switch (notification.type) {
      case 'ACCOUNT_DISABLED':
        return '🚫';
      case 'ACCOUNT_ENABLED':
        return '✅';
      case 'SESSION_REVOKED':
        return '⚠️';
      default:
        return 'ℹ️';
    }
  };

  return (
    <div className={`notification-banner ${getNotificationClass()} ${isVisible ? 'show' : ''}`}>
      <div className="notification-content">
        <span className="notification-icon">{getIcon()}</span>
        <div className="notification-text">
          <strong>{notification.type.replace(/_/g, ' ')}</strong>
          <p>{notification.message}</p>
        </div>
        <button className="notification-close" onClick={handleClose}>
          ×
        </button>
      </div>
    </div>
  );
};

export default NotificationBanner;
