import './ConnectionIndicator.css';

/**
 * Visual indicator for SSE connection status
 */
const ConnectionIndicator = ({ connectionState }) => {
  const getStatusConfig = () => {
    switch (connectionState) {
      case 'connected':
        return {
          label: 'Live',
          color: '#28a745',
          pulse: true
        };
      case 'connecting':
        return {
          label: 'Connecting...',
          color: '#ffc107',
          pulse: true
        };
      case 'error':
        return {
          label: 'Error',
          color: '#dc3545',
          pulse: false
        };
      case 'disconnected':
      default:
        return {
          label: 'Offline',
          color: '#6c757d',
          pulse: false
        };
    }
  };

  const config = getStatusConfig();

  return (
    <div className="connection-indicator">
      <div 
        className={`status-dot ${config.pulse ? 'pulse' : ''}`}
        style={{ backgroundColor: config.color }}
      />
      <span className="status-label">{config.label}</span>
    </div>
  );
};

export default ConnectionIndicator;
