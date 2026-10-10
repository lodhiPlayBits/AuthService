import { Info, ShieldCheck, Zap, Layers, Server } from 'lucide-react';

export default function About() {
  return (
    <div className="animate-fade-in" style={{ maxWidth: '900px', margin: '0 auto', padding: '2rem 0' }}>
      
      <div style={{ textAlign: 'center', marginBottom: '4rem' }}>
        <div style={{ display: 'inline-flex', justifyContent: 'center', alignItems: 'center', width: '64px', height: '64px', borderRadius: '20px', background: 'var(--gradient-hero)', boxShadow: 'var(--glow-primary)', marginBottom: '1.5rem' }}>
          <Info size={32} color="white" />
        </div>
        <h1 style={{ fontSize: '3rem', marginBottom: '1rem' }}>About AuthVolt</h1>
        <p style={{ color: 'var(--text-secondary)', fontSize: '1.25rem', lineHeight: '1.6', maxWidth: '700px', margin: '0 auto' }}>
          AuthVolt is a state-of-the-art identity management solution. Built natively for scale, security, and exceptional user experiences.
        </p>
      </div>

      <div className="glass-panel" style={{ marginBottom: '3rem', padding: '3rem' }}>
        <h2 style={{ marginBottom: '2rem', textAlign: 'center' }}>Enterprise Grade Architecture</h2>
        
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: '2rem' }}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            <div style={{ width: '48px', height: '48px', borderRadius: '12px', background: 'rgba(129, 140, 248, 0.1)', color: 'var(--accent-primary)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <ShieldCheck size={24} />
            </div>
            <h3 style={{ fontSize: '1.25rem', margin: 0 }}>Stateless JWTs</h3>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem', lineHeight: '1.6' }}>Secure, distributed authentication with robust refresh token families and automatic rotation.</p>
          </div>
          
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            <div style={{ width: '48px', height: '48px', borderRadius: '12px', background: 'rgba(34, 211, 238, 0.1)', color: 'var(--accent-tertiary)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <Zap size={24} />
            </div>
            <h3 style={{ fontSize: '1.25rem', margin: 0 }}>Event Streaming</h3>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem', lineHeight: '1.6' }}>Real-time Kafka event streaming architecture for high-throughput, decoupled microservices.</p>
          </div>
          
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            <div style={{ width: '48px', height: '48px', borderRadius: '12px', background: 'rgba(167, 139, 250, 0.1)', color: 'var(--accent-secondary)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <Layers size={24} />
            </div>
            <h3 style={{ fontSize: '1.25rem', margin: 0 }}>SSE & WebSockets</h3>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem', lineHeight: '1.6' }}>Instant push notifications via Server-Sent Events (SSE) and STOMP over WebSockets.</p>
          </div>
          
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            <div style={{ width: '48px', height: '48px', borderRadius: '12px', background: 'rgba(16, 185, 129, 0.1)', color: 'var(--success)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <Server size={24} />
            </div>
            <h3 style={{ fontSize: '1.25rem', margin: 0 }}>Modern Stack</h3>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem', lineHeight: '1.6' }}>Built with Spring Boot 3, Redis, PostgreSQL, and a completely glassmorphic React frontend.</p>
          </div>
        </div>
      </div>
    </div>
  );
}
