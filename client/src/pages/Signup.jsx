import { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useNavigate, Link } from 'react-router-dom';
import { UserPlus } from 'lucide-react';
import { GoogleLogin } from '@react-oauth/google';

export default function Signup() {
  const [name, setName] = useState('');
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [gender, setGender] = useState('MALE'); // default value
  const [phoneNumber, setPhoneNumber] = useState('');
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    try {
      // Call the real backend register endpoint
      const res = await fetch('/api/v1/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: name,
          username: username,
          email: email,
          password: password,
          gender: gender,
          phoneNumber: phoneNumber
        })
      });

      if (!res.ok) {
        const error = await res.json().catch(() => ({ message: 'Registration failed' }));
        alert(error.message || 'Registration failed. Please try again.');
        return;
      }

      const userData = await res.json();
      
      // After successful registration, log in automatically
      // (Or redirect to login page - depending on your preference)
      alert('Registration successful! Please log in.');
      navigate('/login');
      
    } catch (err) {
      console.error('Signup error:', err);
      alert('Network error. Please try again.');
    }
  };

  return (
    <div className="animate-fade-in" style={{ display: 'flex', justifyContent: 'center', marginTop: '4rem', paddingBottom: '2rem' }}>
      <div className="glass-panel" style={{ width: '100%', maxWidth: '400px' }}>
        <div style={{ textAlign: 'center', marginBottom: '2rem' }}>
          <h2 style={{ fontSize: '2rem', marginBottom: '0.5rem' }}>Create Account</h2>
          <p style={{ color: 'var(--text-secondary)' }}>Join AuthVolt today.</p>
        </div>
        
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label">Full Name</label>
            <input type="text" className="form-input" required value={name} onChange={e => setName(e.target.value)} placeholder="John Doe" />
          </div>

          <div className="form-group">
            <label className="form-label">Username</label>
            <input type="text" className="form-input" required value={username} onChange={e => setUsername(e.target.value)} placeholder="johndoe" />
          </div>

          <div className="form-group">
            <label className="form-label">Email Address</label>
            <input type="email" className="form-input" required value={email} onChange={e => setEmail(e.target.value)} placeholder="you@example.com" />
          </div>
          
          <div className="form-group">
            <label className="form-label">Password</label>
            <input type="password" className="form-input" required value={password} onChange={e => setPassword(e.target.value)} placeholder="••••••••" minLength={6} />
          </div>

          <div className="form-group">
            <label className="form-label">Phone Number</label>
            <input type="tel" className="form-input" required value={phoneNumber} onChange={e => setPhoneNumber(e.target.value)} placeholder="1234567890" pattern="[0-9]{10}" title="Must be a 10 digit number" />
          </div>

          <div className="form-group">
            <label className="form-label">Gender</label>
            <select className="form-input" required value={gender} onChange={e => setGender(e.target.value)}>
              <option value="MALE">Male</option>
              <option value="FEMALE">Female</option>
              <option value="OTHER">Other</option>
            </select>
          </div>
          
          <button type="submit" className="btn btn-primary" style={{ width: '100%', marginTop: '1rem', padding: '0.875rem' }}>
            <UserPlus size={18} /> Sign Up
          </button>
        </form>
        
        <div style={{ display: 'flex', alignItems: 'center', margin: '1.5rem 0' }}>
          <div style={{ flex: 1, height: '1px', background: 'var(--glass-border)' }}></div>
          <span style={{ margin: '0 1rem', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>or sign up with</span>
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
                  throw new Error('Google registration failed');
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
                console.error('Google signup error:', err);
                alert('Google registration failed. Please try again.');
              }
            }}
            onError={() => {
              console.log('Login Failed');
            }}
            theme="filled_black"
            shape="rectangular"
            size="large"
            text="signup_with"
          />
        </div>
        
        <p style={{ textAlign: 'center', marginTop: '1.5rem', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
          Already have an account? <Link to="/login" style={{ color: 'var(--accent-primary)', textDecoration: 'none' }}>Log in</Link>
        </p>
      </div>
    </div>
  );
}
