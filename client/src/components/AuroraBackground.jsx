import './AuroraBackground.css';

export default function AuroraBackground({ children }) {
  return (
    <div className="aurora-wrapper">
      <div className="aurora-bg">
        <div className="aurora-gradient aurora-1"></div>
        <div className="aurora-gradient aurora-2"></div>
        <div className="aurora-gradient aurora-3"></div>
        <div className="aurora-particles"></div>
      </div>
      {children}
    </div>
  );
}
