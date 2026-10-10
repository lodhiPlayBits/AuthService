import { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useNavigate, Link } from 'react-router-dom';
import { LogIn, ArrowRight } from 'lucide-react';
import { GoogleLogin } from '@react-oauth/google';

export default function Login() {
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setErrorMsg('');
    
    try {
      const res = await fetch('/api/v1/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ identifier, password })
      });

      if (!res.ok) {
        const error = await res.json().catch(() => ({ message: 'Login failed' }));
        setErrorMsg(error.message || 'Invalid credentials');
        setLoading(false);
        return;
      }

      const data = await res.json();
      const userRole = data.user.roles?.[0]?.roleName || 'USER';
      
      const userData = {
        id: data.user.id,
        username: data.user.name,
        name: data.user.name,
        email: data.user.email,
        phoneNumber: data.user.phoneNumber,
        gender: data.user.gender,
        role: userRole,
        profileComplete: data.user.profileComplete
      };
      
      login(userData, data.accessToken);
      navigate('/dashboard');
    } catch (err) {
      console.error('Login error:', err);
      setErrorMsg('Network error. Please try again.');
      setLoading(false);
    }
  };

  return (
    <div className="animate-fade-in" style={{ display: 'flex', minHeight: '80vh' }}>
      {/* Decorative Left Side */}
      <div style={{ flex: 1, display: 'none', '@media (min-width: 900px)': { display: 'flex' }, flexDirection: 'column', justifyContent: 'center', padding: '4rem', position: 'relative' }}>
        <div style={{ position: 'relative', zIndex: 10, maxWidth: '400px' }}>
          <h2 style={{ fontSize: '3rem', marginBottom: '1rem', fontWeight: 800 }}>Welcome<br/>Back</h2>
          <p style={{ color: 'var(--text-secondary)', fontSize: '1.125rem', lineHeight: 1.6 }}>
            Sign in to continue to AuthVolt. The next generation of identity management.
          </p>
        </div>
      </div>

      {/* Form Right Side */}
      <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '2rem' }}>
        <div className="glass-panel" style={{ width: '100%', maxWidth: '440px', padding: '3rem 2.5rem' }}>
          <div style={{ textAlign: 'center', marginBottom: '2.5rem' }}>
            <h2 style={{ fontSize: '2rem', marginBottom: '0.5rem' }}>Sign In</h2>
          </div>
          
          {errorMsg && (
            <div style={{ padding: '0.75rem', background: 'rgba(239, 68, 68, 0.1)', border: '1px solid var(--danger)', borderRadius: '12px', color: 'var(--danger)', marginBottom: '1.5rem', fontSize: '0.875rem', textAlign: 'center' }}>
              {errorMsg}
            </div>
          )}
          
          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label className="form-label">Email or Username</label>
              <input type="text" className="form-input" required value={identifier} onChange={e => setIdentifier(e.target.value)} placeholder="you@example.com" />
            </div>
            
            <div className="form-group">
              <label className="form-label">Password</label>
              <input type="password" className="form-input" required value={password} onChange={e => setPassword(e.target.value)} placeholder="••••••••" />
            </div>
            
            <button type="submit" className="btn btn-primary" style={{ width: '100%', marginTop: '1rem', padding: '1rem' }} disabled={loading}>
              {loading ? 'Signing in...' : <><LogIn size={18} /> Sign In</>}
            </button>
          </form>
          
          <div style={{ display: 'flex', alignItems: 'center', margin: '2rem 0' }}>
            <div style={{ flex: 1, height: '1px', background: 'var(--glass-border)' }}></div>
            <span style={{ margin: '0 1rem', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>or continue with</span>
            <div style={{ flex: 1, height: '1px', background: 'var(--glass-border)' }}></div>
          </div>

          <div style={{ display: 'flex', justifyContent: 'center' }}>
            <GoogleLogin
              onSuccess={async credentialResponse => {
                try {
                  const res = await fetch('/api/v1/auth/oauth2/google', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    credentials: 'include',
                    body: JSON.stringify({ idToken: credentialResponse.credential })
                  });

                  if (!res.ok) throw new Error('Google authentication failed');
                  const data = await res.json();
                  const userRole = data.user.roles?.[0]?.roleName || 'USER';
                  
                  const userData = {
                    id: data.user.id, username: data.user.name, name: data.user.name,
                    email: data.user.email, phoneNumber: data.user.phoneNumber,
                    gender: data.user.gender, role: userRole, profileComplete: data.user.profileComplete
                  };
                  
                  login(userData, data.accessToken);
                  navigate(data.profileComplete ? '/dashboard' : '/edit-profile');
                } catch (err) {
                  console.error('Google login error:', err);
                  setErrorMsg('Google authentication failed.');
                }
              }}
              onError={() => setErrorMsg('Google login failed.')}
              theme="filled_black" shape="rectangular" size="large"
            />
          </div>
          
          <p style={{ textAlign: 'center', marginTop: '2rem', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
            Don't have an account? <Link to="/signup" style={{ color: 'var(--accent-primary)', textDecoration: 'none', fontWeight: 600, display: 'inline-flex', alignItems: 'center', gap: '0.25rem' }}>Sign up <ArrowRight size={14}/></Link>
          </p>
        </div>
      </div>
      
      <style>{`
        @media (max-width: 899px) {
          .animate-fade-in > div:first-child { display: none !important; }
        }
      `}</style>
    </div>
  );
}
