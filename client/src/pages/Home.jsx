import { Link } from 'react-router-dom';
import { Shield, Zap, Lock } from 'lucide-react';

export default function Home() {
  return (
    <div className="animate-fade-in" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', minHeight: '70vh', textAlign: 'center' }}>
      <div style={{ display: 'inline-flex', padding: '0.5rem 1rem', background: 'rgba(99, 102, 241, 0.1)', color: 'var(--accent-primary)', borderRadius: '999px', fontSize: '0.875rem', fontWeight: '500', marginBottom: '2rem' }}>
        🚀 AuthVolt v2.0 is Live
      </div>
      
      <h1 style={{ fontSize: '4rem', maxWidth: '800px', lineHeight: '1.1' }}>
        Next-Generation <span className="text-gradient">Authentication</span>
      </h1>
      
      <p style={{ color: 'var(--text-secondary)', fontSize: '1.25rem', maxWidth: '600px', margin: '1.5rem 0 3rem 0' }}>
        Secure, fast, and beautiful identity management with real-time SSE notifications and role-based access control.
      </p>
      
      <div style={{ display: 'flex', gap: '1rem' }}>
        <Link to="/signup" className="btn btn-primary" style={{ padding: '1rem 2rem', fontSize: '1.125rem' }}>Get Started</Link>
        <Link to="/about" className="glass-panel" style={{ padding: '1rem 2rem', textDecoration: 'none', color: 'var(--text-primary)', borderRadius: '8px', display: 'inline-flex', alignItems: 'center' }}>Learn More</Link>
      </div>
      
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '2rem', marginTop: '5rem' }}>
        {[
          { icon: <Shield size={24}/>, title: 'Secure by Default', desc: 'Industry standard encryption and JWTs.' },
          { icon: <Zap size={24}/>, title: 'Lightning Fast', desc: 'Optimized performance with Redis caching.' },
          { icon: <Lock size={24}/>, title: 'Role Based', desc: 'Granular permissions for Admins and Users.' }
        ].map((feature, i) => (
          <div key={i} className="glass-panel" style={{ textAlign: 'left', padding: '1.5rem' }}>
            <div style={{ color: 'var(--accent-primary)', marginBottom: '1rem' }}>{feature.icon}</div>
            <h3 style={{ fontSize: '1.25rem' }}>{feature.title}</h3>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>{feature.desc}</p>
          </div>
        ))}
      </div>
    </div>
  );
}
