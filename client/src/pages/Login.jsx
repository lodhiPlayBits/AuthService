import { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useNavigate, Link } from 'react-router-dom';
import { LogIn } from 'lucide-react';
import { GoogleLogin } from '@react-oauth/google';

export default function Login() {
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    try {
      // First, fetch CSRF token by making any GET request
      // This triggers the CsrfCookieFilter to set the XSRF-TOKEN cookie
      await fetch('/api/v1/auth/login', {
        method: 'OPTIONS',
        credentials: 'include'
      }).catch(() => {}); // Ignore errors, just need to trigger CSRF cookie
      
      // Now make the actual login request
      const res = await fetch('/api/v1/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include', // Important: to receive cookies
        body: JSON.stringify({
          identifier: identifier,
          password: password
        })
      });

      if (!res.ok) {
        const error = await res.json().catch(() => ({ message: 'Login failed' }));
        alert(error.message || 'Invalid credentials');
        return;
      }

      const data = await res.json();
      
      // data contains: { accessToken, user: { id, email, name, roles }, profileComplete }
      // Extract role from roles array (backend uses 'roleName' property)
      const userRole = data.user.roles?.[0]?.roleName || 'USER';
      
      const userData = {
        id: data.user.id,
        username: data.user.name, // Backend uses 'name' not 'username'
        name: data.user.name,
        email: data.user.email,
        phoneNumber: data.user.phoneNumber,
        gender: data.user.gender,
        role: userRole,
        profileComplete: data.user.profileComplete
      };
      
      // Store accessToken and user data
      login(userData, data.accessToken);
      navigate('/dashboard');
    } catch (err) {
      console.error('Login error:', err);
      alert('Network error. Please try again.');
    }
  };

  return (
    <div className="animate-fade-in" style={{ display: 'flex', justifyContent: 'center', marginTop: '4rem' }}>
      <div className="glass-panel" style={{ width: '100%', maxWidth: '400px' }}>
        <div style={{ textAlign: 'center', marginBottom: '2rem' }}>
          <h2 style={{ fontSize: '2rem', marginBottom: '0.5rem' }}>Welcome Back</h2>
          <p style={{ color: 'var(--text-secondary)' }}>Sign in to continue to AuthVolt.</p>
        </div>
        
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label">Identifier (Email, Username, or Phone)</label>
            <input type="text" className="form-input" required value={identifier} onChange={e => setIdentifier(e.target.value)} placeholder="you@example.com, johndoe, etc." />
          </div>
          
          <div className="form-group">
            <label className="form-label">Password</label>
            <input type="password" className="form-input" required value={password} onChange={e => setPassword(e.target.value)} placeholder="••••••••" />
          </div>
          
          <button type="submit" className="btn btn-primary" style={{ width: '100%', marginTop: '1rem', padding: '0.875rem' }}>
            <LogIn size={18} /> Sign In
          </button>
        </form>
        
        <div style={{ display: 'flex', alignItems: 'center', margin: '1.5rem 0' }}>
          <div style={{ flex: 1, height: '1px', background: 'var(--glass-border)' }}></div>
          <span style={{ margin: '0 1rem', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>or continue with</span>
          <div style={{ flex: 1, height: '1px', background: 'var(--glass-border)' }}></div>
        </div>

        <div style={{ display: 'flex', justifyContent: 'center' }}>
          <GoogleLogin
            onSuccess={async credentialResponse => {
              try {
                // First trigger CSRF cookie
                await fetch('/api/v1/auth/login', { method: 'OPTIONS' }).catch(() => {});
                
                const res = await fetch('/api/v1/auth/oauth2/google', {
                  method: 'POST',
                  headers: { 'Content-Type': 'application/json' },
                  credentials: 'include',
                  body: JSON.stringify({ idToken: credentialResponse.credential })
                });

                if (!res.ok) {
                  throw new Error('Google authentication failed');
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
                
                if (!data.profileComplete) {
                  navigate('/edit-profile');
                } else {
                  navigate('/dashboard');
                }
              } catch (err) {
                console.error('Google login error:', err);
                alert('Google authentication failed. Please try again.');
              }
            }}
            onError={() => {
              console.log('Login Failed');
            }}
            theme="filled_black"
            shape="rectangular"
            size="large"
          />
        </div>
        
        <p style={{ textAlign: 'center', marginTop: '1.5rem', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
          Don't have an account? <Link to="/signup" style={{ color: 'var(--accent-primary)', textDecoration: 'none' }}>Sign up</Link>
        </p>
      </div>
    </div>
  );
}
