import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Zap, LogOut, User, Menu, X, Bell } from 'lucide-react';
import ConnectionIndicator from './ConnectionIndicator';
import { useState, useEffect } from 'react';

export default function Navbar({ connectionState }) {
  const { isAuthenticated, user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [scrolled, setScrolled] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  useEffect(() => {
    const handleScroll = () => setScrolled(window.scrollY > 20);
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  // Close mobile menu on navigation
  useEffect(() => {
    setMobileMenuOpen(false);
  }, [location.pathname]);

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <>
      <nav style={{
        position: 'sticky',
        top: 0,
        zIndex: 50,
        padding: '1rem 2rem',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        background: scrolled ? 'rgba(3, 7, 18, 0.7)' : 'transparent',
        backdropFilter: scrolled ? 'blur(16px)' : 'none',
        WebkitBackdropFilter: scrolled ? 'blur(16px)' : 'none',
        borderBottom: scrolled ? '1px solid var(--glass-border)' : '1px solid transparent',
        transition: 'all 0.3s ease'
      }}>
        <Link to="/" style={{ textDecoration: 'none', display: 'flex', alignItems: 'center', gap: '0.75rem', color: 'var(--text-primary)', fontWeight: '800', fontSize: '1.5rem', fontFamily: 'Outfit, sans-serif' }}>
          <div style={{
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            background: 'var(--gradient-hero)', padding: '0.5rem', borderRadius: '12px',
            boxShadow: 'var(--glow-primary)'
          }}>
            <Zap color="white" size={24} fill="currentColor" />
          </div>
          AuthVolt
        </Link>
        
        {/* Desktop Menu */}
        <div style={{ display: 'none', gap: '2rem', alignItems: 'center', '@media (min-width: 768px)': { display: 'flex' } }} className="desktop-menu">
          <Link to="/about" style={{ color: location.pathname === '/about' ? 'var(--text-primary)' : 'var(--text-secondary)', textDecoration: 'none', fontWeight: 500, transition: 'color 0.2s' }}>About</Link>
          
          {isAuthenticated ? (
            <>
              <Link to="/dashboard" style={{ color: location.pathname === '/dashboard' ? 'var(--text-primary)' : 'var(--text-secondary)', textDecoration: 'none', fontWeight: 500, transition: 'color 0.2s' }}>Dashboard</Link>
              <ConnectionIndicator connectionState={connectionState} />
              <div style={{ display: 'flex', alignItems: 'center', gap: '1.5rem', marginLeft: '1rem', paddingLeft: '1.5rem', borderLeft: '1px solid var(--glass-border)' }}>
                <Link to="/edit-profile" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.875rem', color: 'var(--text-primary)', textDecoration: 'none', fontWeight: 500 }}>
                  <div style={{ width: '32px', height: '32px', borderRadius: '50%', background: 'var(--gradient-hero)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <User size={16} color="white" />
                  </div>
                  {user?.username} <span style={{ opacity: 0.5 }}>({user?.role})</span>
                </Link>
                <button onClick={handleLogout} className="btn" style={{ padding: '0.5rem 1rem', background: 'rgba(255,255,255,0.05)', border: '1px solid var(--glass-border)', color: 'var(--text-secondary)' }}>
                  <LogOut size={16} /> Logout
                </button>
              </div>
            </>
          ) : (
            <div style={{ display: 'flex', gap: '1rem', marginLeft: '1rem' }}>
              <Link to="/login" className="btn" style={{ background: 'transparent', color: 'var(--text-primary)' }}>Log In</Link>
              <Link to="/signup" className="btn btn-primary">Sign Up</Link>
            </div>
          )}
        </div>

        {/* Mobile Toggle */}
        <button className="mobile-toggle" style={{ background: 'none', border: 'none', color: 'var(--text-primary)', cursor: 'pointer' }} onClick={() => setMobileMenuOpen(!mobileMenuOpen)}>
          {mobileMenuOpen ? <X size={28} /> : <Menu size={28} />}
        </button>
      </nav>

      {/* Mobile Menu Overlay */}
      {mobileMenuOpen && (
        <div style={{
          position: 'fixed', top: '72px', left: 0, right: 0, bottom: 0,
          background: 'rgba(3, 7, 18, 0.95)', backdropFilter: 'blur(20px)',
          zIndex: 40, padding: '2rem', display: 'flex', flexDirection: 'column', gap: '1.5rem',
          borderTop: '1px solid var(--glass-border)'
        }}>
          <Link to="/about" style={{ fontSize: '1.25rem', color: 'var(--text-primary)', textDecoration: 'none' }}>About</Link>
          {isAuthenticated ? (
            <>
              <Link to="/dashboard" style={{ fontSize: '1.25rem', color: 'var(--text-primary)', textDecoration: 'none' }}>Dashboard</Link>
              <Link to="/edit-profile" style={{ fontSize: '1.25rem', color: 'var(--text-primary)', textDecoration: 'none' }}>Profile ({user?.username})</Link>
              <div style={{ margin: '1rem 0' }}>
                <ConnectionIndicator connectionState={connectionState} />
              </div>
              <button onClick={handleLogout} className="btn btn-danger" style={{ width: '100%', justifyContent: 'center' }}>
                <LogOut size={18} /> Logout
              </button>
            </>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', marginTop: '1rem' }}>
              <Link to="/login" className="btn" style={{ background: 'rgba(255,255,255,0.05)', color: 'var(--text-primary)', justifyContent: 'center' }}>Log In</Link>
              <Link to="/signup" className="btn btn-primary" style={{ justifyContent: 'center' }}>Sign Up</Link>
            </div>
          )}
        </div>
      )}

      <style>{`
        .desktop-menu { display: none !important; }
        .mobile-toggle { display: block; }
        @media (min-width: 768px) {
          .desktop-menu { display: flex !important; }
          .mobile-toggle { display: none !important; }
        }
      `}</style>
    </>
  );
}
