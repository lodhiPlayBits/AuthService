import { Link } from 'react-router-dom';
import { Shield, Zap, Lock, Code, ChevronRight, Activity } from 'lucide-react';

export default function Home() {
  return (
    <div className="animate-fade-in" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', minHeight: '80vh', padding: '4rem 0' }}>
      
      {/* Hero Section */}
      <div style={{ textAlign: 'center', marginBottom: '6rem', position: 'relative', zIndex: 10 }}>
        <div className="stagger-1 animate-fade-in" style={{ display: 'inline-flex', padding: '0.5rem 1rem', background: 'rgba(129, 140, 248, 0.1)', color: 'var(--accent-primary)', borderRadius: '999px', fontSize: '0.875rem', fontWeight: '600', marginBottom: '2rem', border: '1px solid rgba(129, 140, 248, 0.2)' }}>
          <Zap size={16} style={{ marginRight: '0.5rem' }} /> AuthVolt v2.0 is Live
        </div>
        
        <h1 className="stagger-2 animate-fade-in" style={{ fontSize: 'clamp(3rem, 8vw, 5rem)', maxWidth: '900px', lineHeight: '1.1', margin: '0 auto 1.5rem', fontWeight: '800' }}>
          Next-Generation <span className="text-gradient">Authentication</span>
        </h1>
        
        <p className="stagger-3 animate-fade-in" style={{ color: 'var(--text-secondary)', fontSize: 'clamp(1.125rem, 3vw, 1.25rem)', maxWidth: '600px', margin: '0 auto 3rem', lineHeight: '1.6' }}>
          Secure, seamless identity management for modern platforms. Passwordless, MFA, and robust developer APIs.
        </p>
        
        <div className="stagger-3 animate-fade-in" style={{ display: 'flex', gap: '1rem', justifyContent: 'center', flexWrap: 'wrap' }}>
          <Link to="/signup" className="btn btn-primary" style={{ padding: '1rem 2.5rem', fontSize: '1.125rem' }}>
            Get Started for Free <ChevronRight size={18} />
          </Link>
          <Link to="/about" className="btn" style={{ padding: '1rem 2.5rem', fontSize: '1.125rem', background: 'rgba(255,255,255,0.05)', border: '1px solid var(--glass-border)' }}>
            Explore Docs
          </Link>
        </div>
      </div>
      
      {/* Features Grid */}
      <div className="stagger-3 animate-fade-in" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '2rem', width: '100%', maxWidth: '1200px' }}>
        {[
          { icon: <Shield size={28}/>, title: 'Secure by Default', desc: 'Industry standard encryption, HTTP-only cookies, and stateless JWT architecture.', color: 'var(--accent-primary)' },
          { icon: <Activity size={28}/>, title: 'Real-time Events', desc: 'Instant push notifications via SSE and WebSockets for seamless UX.', color: 'var(--accent-tertiary)' },
          { icon: <Lock size={28}/>, title: 'Role Based Access', desc: 'Granular permissions and complete administrative control out of the box.', color: 'var(--accent-secondary)' },
          { icon: <Code size={28}/>, title: 'Developer First', desc: 'Clean APIs, comprehensive documentation, and easy integration.', color: 'var(--success)' }
        ].map((feature, i) => (
          <div key={i} className="glass-panel" style={{ textAlign: 'left', padding: '2rem' }}>
            <div style={{ color: feature.color, marginBottom: '1.5rem', display: 'inline-flex', padding: '1rem', background: `color-mix(in srgb, ${feature.color} 15%, transparent)`, borderRadius: '16px' }}>
              {feature.icon}
            </div>
            <h3 style={{ fontSize: '1.25rem', marginBottom: '0.75rem' }}>{feature.title}</h3>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem', lineHeight: '1.6' }}>{feature.desc}</p>
          </div>
        ))}
      </div>
    </div>
  );
}
