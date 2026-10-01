import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Zap, LogOut, User } from 'lucide-react';
import ConnectionIndicator from './ConnectionIndicator';

export default function Navbar({ connectionState }) {
  const { isAuthenticated, user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <nav style={{ padding: '1.5rem 2rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid var(--glass-border)', background: 'var(--glass-bg)', backdropFilter: 'blur(10px)' }}>
      <Link to="/" style={{ textDecoration: 'none', display: 'flex', alignItems: 'center', gap: '0.5rem', color: 'var(--text-primary)', fontWeight: '700', fontSize: '1.25rem' }}>
        <Zap color="var(--accent-primary)" /> AuthVolt
      </Link>
      
      <div style={{ display: 'flex', gap: '1.5rem', alignItems: 'center' }}>
        <Link to="/about" style={{ color: 'var(--text-secondary)', textDecoration: 'none' }}>About</Link>
        
        {isAuthenticated ? (
          <>
            <Link to="/dashboard" style={{ color: 'var(--text-secondary)', textDecoration: 'none' }}>Dashboard</Link>
            <ConnectionIndicator connectionState={connectionState} />
            <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginLeft: '1rem', paddingLeft: '1rem', borderLeft: '1px solid var(--glass-border)' }}>
              <span style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.875rem' }}>
                <User size={16} /> {user?.username} ({user?.role})
              </span>
              <button onClick={handleLogout} className="btn btn-danger" style={{ padding: '0.5rem 1rem' }}>
                <LogOut size={16} /> Logout
              </button>
            </div>
          </>
        ) : (
          <div style={{ display: 'flex', gap: '1rem', marginLeft: '1rem' }}>
            <Link to="/login" style={{ color: 'var(--text-primary)', textDecoration: 'none', padding: '0.5rem 1rem' }}>Login</Link>
            <Link to="/signup" className="btn btn-primary">Sign Up</Link>
          </div>
        )}
      </div>
    </nav>
  );
}
