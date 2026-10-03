import { useEffect, useState } from 'react';
import './NotificationBanner.css';
import { ShieldAlert, Info, Bell, ShieldCheck, X } from 'lucide-react';

const PERSISTENT_EVENTS = ['ACCOUNT_DISABLED', 'SESSION_REVOKED', 'ANNOUNCEMENT'];
const DURATION_MS = 5000;

export default function NotificationBanner({ notification, onClose, onAcknowledge, onDismiss }) {
  const [isVisible, setIsVisible] = useState(false);
  const [progress, setProgress] = useState(100);

  useEffect(() => {
    const frame = requestAnimationFrame(() => setIsVisible(true));
    return () => cancelAnimationFrame(frame);
  }, []);

  const close = () => {
    setIsVisible(false);
    setTimeout(() => onClose?.(), 300);
  };

  useEffect(() => {
    if (!notification || PERSISTENT_EVENTS.includes(notification.type)) return;
    
    const startTime = Date.now();
    let animFrame;

    const tick = () => {
      const elapsed = Date.now() - startTime;
      const remaining = Math.max(0, 100 - (elapsed / DURATION_MS) * 100);
      setProgress(remaining);
      
      if (remaining > 0) {
        animFrame = requestAnimationFrame(tick);
      } else {
        close();
      }
    };
    
    animFrame = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(animFrame);
  }, [notification, onClose]);

  if (!notification) return null;

  const getConfig = () => {
    switch (notification.type) {
      case 'ACCOUNT_DISABLED':
      case 'SESSION_REVOKED':
        return { class: 'toast-error', icon: <ShieldAlert size={20} /> };
      case 'ACCOUNT_ENABLED':
        return { class: 'toast-success', icon: <ShieldCheck size={20} /> };
      case 'ANNOUNCEMENT':
        return { class: 'toast-accent', icon: <Bell size={20} /> };
      default:
        return { class: 'toast-info', icon: <Info size={20} /> };
    }
  };

  const config = getConfig();
  const heading = notification.type === 'ANNOUNCEMENT'
    ? (notification.title || 'Announcement')
    : (notification.type ? notification.type.replace(/_/g, ' ') : 'Notification');

  return (
    <div className={`toast-container ${isVisible ? 'show' : ''}`}>
      <div className={`toast-card ${config.class}`}>
        <div className="toast-icon-wrapper">
          {config.icon}
        </div>
        
        <div className="toast-content">
          <h4 className="toast-title">{heading}</h4>
          <p className="toast-message">{notification.message}</p>
          
          {notification.type === 'ANNOUNCEMENT' && (
            <div className="toast-actions">
              <button className="btn btn-primary btn-sm" onClick={() => { onAcknowledge?.(notification.announcementId); close(); }}>
                Acknowledge
              </button>
              <button className="btn btn-sm" onClick={() => { onDismiss?.(notification.announcementId); close(); }}>
                Dismiss
              </button>
            </div>
          )}
        </div>
        
        <button className="toast-close" onClick={close}>
          <X size={16} />
        </button>

        {!PERSISTENT_EVENTS.includes(notification.type) && (
          <div className="toast-progress-track">
            <div className="toast-progress-bar" style={{ width: `${progress}%` }} />
          </div>
        )}
      </div>
    </div>
  );
}
