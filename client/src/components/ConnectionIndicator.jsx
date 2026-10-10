import './ConnectionIndicator.css';
import { Wifi, WifiOff, Loader, AlertCircle } from 'lucide-react';

export default function ConnectionIndicator({ connectionState }) {
  const getStatusConfig = () => {
    switch (connectionState) {
      case 'connected':
        return { label: 'Connected', className: 'status-connected', icon: <Wifi size={14} /> };
      case 'connecting':
        return { label: 'Connecting', className: 'status-connecting', icon: <Loader size={14} className="spin" /> };
      case 'error':
        return { label: 'Error', className: 'status-error', icon: <AlertCircle size={14} /> };
      case 'disconnected':
      default:
        return { label: 'Offline', className: 'status-offline', icon: <WifiOff size={14} /> };
    }
  };

  const config = getStatusConfig();

  return (
    <div className={`connection-indicator ${config.className}`}>
      <div className="status-ring-container">
        <div className="status-ring-1"></div>
        <div className="status-ring-2"></div>
        <div className="status-dot"></div>
      </div>
      <span className="status-label">{config.icon} {config.label}</span>
    </div>
  );
}
