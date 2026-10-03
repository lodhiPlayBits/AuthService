import { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useNavigate, Link } from 'react-router-dom';
import { UserPlus, ArrowLeft } from 'lucide-react';
import { GoogleLogin } from '@react-oauth/google';

export default function Signup() {
  const [formData, setFormData] = useState({ name: '', username: '', email: '', password: '', gender: 'MALE', phoneNumber: '' });
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleChange = (e) => setFormData({ ...formData, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true); setErrorMsg('');
    try {
      const res = await fetch('/api/v1/auth/register', {
        method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(formData)
      });
      if (!res.ok) {
        const error = await res.json().catch(() => ({ message: 'Registration failed' }));
        setErrorMsg(error.message || 'Registration failed. Please try again.');
        setLoading(false); return;
      }
      navigate('/login');
    } catch (err) {
      console.error('Signup error:', err);
      setErrorMsg('Network error. Please try again.');
      setLoading(false);
    }
  };

  return (
    <div className="animate-fade-in" style={{ display: 'flex', minHeight: '80vh' }}>
      <div style={{ flex: 1, display: 'none', '@media (min-width: 900px)': { display: 'flex' }, flexDirection: 'column', justifyContent: 'center', padding: '4rem', position: 'relative' }}>
        <div style={{ position: 'relative', zIndex: 10, maxWidth: '400px' }}>
          <h2 style={{ fontSize: '3rem', marginBottom: '1rem', fontWeight: 800 }}>Join<br/>AuthVolt</h2>
          <p style={{ color: 'var(--text-secondary)', fontSize: '1.125rem', lineHeight: 1.6 }}>
            Experience secure, seamless identity management designed for modern platforms.
          </p>
        </div>
      </div>

      <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '2rem' }}>
        <div className="glass-panel" style={{ width: '100%', maxWidth: '480px', padding: '3rem 2.5rem' }}>
          <div style={{ textAlign: 'center', marginBottom: '2.5rem' }}>
            <h2 style={{ fontSize: '2rem', marginBottom: '0.5rem' }}>Create Account</h2>
          </div>
          
          {errorMsg && (
            <div style={{ padding: '0.75rem', background: 'rgba(239, 68, 68, 0.1)', border: '1px solid var(--danger)', borderRadius: '12px', color: 'var(--danger)', marginBottom: '1.5rem', fontSize: '0.875rem', textAlign: 'center' }}>
              {errorMsg}
            </div>
          )}
          
          <form onSubmit={handleSubmit} style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
            <div className="form-group" style={{ gridColumn: '1 / -1', marginBottom: '0.5rem' }}>
              <label className="form-label">Full Name</label>
              <input type="text" name="name" className="form-input" required value={formData.name} onChange={handleChange} placeholder="John Doe" />
            </div>
            <div className="form-group" style={{ gridColumn: '1 / -1', marginBottom: '0.5rem' }}>
              <label className="form-label">Email Address</label>
              <input type="email" name="email" className="form-input" required value={formData.email} onChange={handleChange} placeholder="you@example.com" />
            </div>
            <div className="form-group" style={{ marginBottom: '0.5rem' }}>
              <label className="form-label">Username</label>
              <input type="text" name="username" className="form-input" required value={formData.username} onChange={handleChange} placeholder="johndoe" />
            </div>
            <div className="form-group" style={{ marginBottom: '0.5rem' }}>
              <label className="form-label">Phone Number</label>
              <input type="tel" name="phoneNumber" className="form-input" required value={formData.phoneNumber} onChange={handleChange} placeholder="1234567890" pattern="[0-9]{10}" />
            </div>
            <div className="form-group" style={{ marginBottom: '0.5rem' }}>
              <label className="form-label">Password</label>
              <input type="password" name="password" className="form-input" required value={formData.password} onChange={handleChange} placeholder="••••••••" minLength={6} />
            </div>
            <div className="form-group" style={{ marginBottom: '0.5rem' }}>
              <label className="form-label">Gender</label>
              <select name="gender" className="form-input" required value={formData.gender} onChange={handleChange}>
                <option value="MALE">Male</option><option value="FEMALE">Female</option><option value="OTHER">Other</option>
              </select>
            </div>
            
            <button type="submit" className="btn btn-primary" style={{ gridColumn: '1 / -1', width: '100%', marginTop: '1rem', padding: '1rem' }} disabled={loading}>
              {loading ? 'Creating...' : <><UserPlus size={18} /> Sign Up</>}
            </button>
          </form>
          
          <div style={{ display: 'flex', alignItems: 'center', margin: '2rem 0' }}>
            <div style={{ flex: 1, height: '1px', background: 'var(--glass-border)' }}></div>
            <span style={{ margin: '0 1rem', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>or sign up with</span>
            <div style={{ flex: 1, height: '1px', background: 'var(--glass-border)' }}></div>
          </div>

          <div style={{ display: 'flex', justifyContent: 'center' }}>
            <GoogleLogin
              onSuccess={async credentialResponse => {
                try {
                  const res = await fetch('/api/v1/auth/oauth2/google', {
                    method: 'POST', headers: { 'Content-Type': 'application/json' }, credentials: 'include', body: JSON.stringify({ idToken: credentialResponse.credential })
                  });
                  if (!res.ok) throw new Error('Google registration failed');
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
                  setErrorMsg('Google registration failed.');
                }
              }}
              onError={() => setErrorMsg('Google login failed.')}
              theme="filled_black" shape="rectangular" size="large" text="signup_with"
            />
          </div>
          
          <p style={{ textAlign: 'center', marginTop: '2rem', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
            Already have an account? <Link to="/login" style={{ color: 'var(--accent-primary)', textDecoration: 'none', fontWeight: 600, display: 'inline-flex', alignItems: 'center', gap: '0.25rem' }}><ArrowLeft size={14}/> Log in</Link>
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
