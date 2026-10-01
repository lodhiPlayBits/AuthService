import { Info } from 'lucide-react';

export default function About() {
  return (
    <div className="animate-fade-in" style={{ display: 'flex', justifyContent: 'center', marginTop: '2rem' }}>
      <div className="glass-panel" style={{ width: '100%', maxWidth: '800px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '2rem', borderBottom: '1px solid var(--glass-border)', paddingBottom: '1rem' }}>
          <Info size={32} color="var(--accent-primary)" />
          <h2>About AuthVolt</h2>
        </div>
        
        <p style={{ color: 'var(--text-secondary)', fontSize: '1.125rem', lineHeight: '1.7', marginBottom: '1.5rem' }}>
          AuthVolt is a state-of-the-art identity management solution. Built natively for scale, security, and exceptional user experiences.
        </p>

        <p style={{ color: 'var(--text-secondary)', fontSize: '1.125rem', lineHeight: '1.7', marginBottom: '1.5rem' }}>
          It leverages robust enterprise technologies under the hood, featuring real-time Kafka event streaming, Server-Sent Events (SSE) for instant push notifications, and a fully glassmorphic React interface.
        </p>

        <div style={{ background: 'rgba(15,23,42,0.5)', padding: '1.5rem', borderRadius: '8px', border: '1px solid var(--glass-border)' }}>
          <h3 style={{ marginBottom: '1rem', color: 'var(--accent-primary)' }}>Core Features</h3>
          <ul style={{ color: 'var(--text-secondary)', marginLeft: '1.5rem', lineHeight: '2' }}>
            <li>JWT & Refresh Token Architecture</li>
            <li>Role-Based Access Control (Admin vs User)</li>
            <li>Real-time SSE Notification Stream</li>
            <li>Modern, fluid Glassmorphism UI</li>
          </ul>
        </div>
      </div>
    </div>
  );
}
