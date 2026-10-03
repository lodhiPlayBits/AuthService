import { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useNavigate, useParams } from 'react-router-dom';
import { User, Mail, ArrowLeft, Save, Lock, Phone, UserCircle, CheckCircle2 } from 'lucide-react';
import { authGet, authPut, authPost } from '../utils/api';

export default function EditProfile() {
  const { user, updateUser } = useAuth();
  const navigate = useNavigate();
  const { userId } = useParams();
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [userData, setUserData] = useState(null);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(false);
  
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [name, setName] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [gender, setGender] = useState('MALE');
  
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [pwdError, setPwdError] = useState(null);
  const [pwdSuccess, setPwdSuccess] = useState(false);

  const targetUserId = userId || user?.id;
  const isEditingSelf = !userId || userId == user?.id;
  const canEdit = isEditingSelf || user?.role === 'ADMIN';

  useEffect(() => {
    if (!user) { navigate('/login'); return; }
    if (!targetUserId || !canEdit) { navigate('/dashboard'); return; }
    let cancelled = false;
    authGet(`/api/v1/users/${targetUserId}`)
      .then(res => { if (!res.ok) throw new Error('Failed to fetch data'); return res.json(); })
      .then(data => {
        if (cancelled) return;
        setUserData(data); setUsername(data.username || ''); setEmail(data.email || '');
        setName(data.name || ''); setPhoneNumber(data.phoneNumber || ''); setGender(data.gender || 'MALE');
      })
      .catch(err => { if (!cancelled) setError(err.message); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [user, targetUserId, canEdit, navigate]);

  const handleSaveProfile = async (e) => {
    e.preventDefault();
    setSaving(true); setError(null); setSuccess(false);
    try {
      const payload = { username, email, name, phoneNumber, gender };
      const res = await authPut(`/api/v1/users/${targetUserId}`, payload);
      if (!res.ok) throw new Error('Failed to update profile');
      setSuccess(true);
      if (isEditingSelf) updateUser(payload);
      setTimeout(() => setSuccess(false), 3000);
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  };

  const handleChangePassword = async (e) => {
    e.preventDefault();
    if (newPassword !== confirmPassword) { setPwdError('Passwords do not match'); return; }
    if (newPassword.length < 6) { setPwdError('Password too short (min 6 chars)'); return; }
    setSaving(true); setPwdError(null); setPwdSuccess(false);
    try {
      const res = await authPost(`/api/v1/users/${targetUserId}/change-password`, { currentPassword, newPassword });
      if (!res.ok) throw new Error('Failed to change password');
      setPwdSuccess(true); setCurrentPassword(''); setNewPassword(''); setConfirmPassword('');
      setTimeout(() => setPwdSuccess(false), 3000);
    } catch (err) {
      setPwdError(err.message);
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <div style={{ textAlign: 'center', padding: '4rem' }}>Loading profile...</div>;

  return (
    <div className="animate-fade-in" style={{ maxWidth: '1000px', margin: '0 auto', paddingBottom: '4rem' }}>
      <button onClick={() => navigate(userId ? '/manage-users' : '/dashboard')} className="btn" style={{ background: 'transparent', border: '1px solid var(--glass-border)', padding: '0.5rem 1rem', fontSize: '0.875rem', marginBottom: '2rem' }}>
        <ArrowLeft size={16} /> Back
      </button>

      <div style={{ display: 'flex', alignItems: 'center', gap: '1.5rem', marginBottom: '3rem' }}>
        <div style={{ width: '80px', height: '80px', borderRadius: '24px', background: 'var(--gradient-hero)', display: 'flex', alignItems: 'center', justifyContent: 'center', boxShadow: 'var(--glow-primary)' }}>
          <UserCircle size={40} color="white" />
        </div>
        <div>
          <h1 style={{ margin: 0, fontSize: '2.5rem' }}>{isEditingSelf ? 'Settings' : `Editing ${userData?.username}`}</h1>
          <p style={{ color: 'var(--text-secondary)' }}>Manage account details and security preferences.</p>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr', gap: '2rem', '@media (min-width: 768px)': { gridTemplateColumns: '1fr 1fr' } }}>
        
        {/* Profile Info */}
        <div className="glass-panel" style={{ height: 'fit-content' }}>
          <h2 style={{ fontSize: '1.25rem', marginBottom: '1.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <User size={20} color="var(--accent-primary)" /> Profile Information
          </h2>
          
          {error && <div style={{ padding: '0.75rem', background: 'rgba(239, 68, 68, 0.1)', color: 'var(--danger)', borderRadius: '8px', marginBottom: '1.5rem', fontSize: '0.875rem' }}>{error}</div>}
          {success && <div style={{ padding: '0.75rem', background: 'rgba(16, 185, 129, 0.1)', color: 'var(--success)', borderRadius: '8px', marginBottom: '1.5rem', fontSize: '0.875rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}><CheckCircle2 size={16}/> Saved successfully</div>}

          <form onSubmit={handleSaveProfile} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
            <div className="form-group" style={{ margin: 0 }}>
              <label className="form-label">Full Name</label>
              <input type="text" className="form-input" value={name} onChange={e => setName(e.target.value)} required />
            </div>
            <div className="form-group" style={{ margin: 0 }}>
              <label className="form-label">Username</label>
              <input type="text" className="form-input" value={username} onChange={e => setUsername(e.target.value)} required />
            </div>
            <div className="form-group" style={{ margin: 0 }}>
              <label className="form-label">Email</label>
              <input type="email" className="form-input" value={email} onChange={e => setEmail(e.target.value)} required />
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
              <div className="form-group" style={{ margin: 0 }}>
                <label className="form-label">Phone</label>
                <input type="tel" className="form-input" value={phoneNumber} onChange={e => setPhoneNumber(e.target.value)} required pattern="[0-9]{10}" />
              </div>
              <div className="form-group" style={{ margin: 0 }}>
                <label className="form-label">Gender</label>
                <select className="form-input" value={gender} onChange={e => setGender(e.target.value)}>
                  <option value="MALE">Male</option><option value="FEMALE">Female</option><option value="OTHER">Other</option>
                </select>
              </div>
            </div>
            <button type="submit" className="btn btn-primary" disabled={saving} style={{ marginTop: '0.5rem' }}>
              <Save size={18} /> Save Changes
            </button>
          </form>
        </div>

        {/* Security */}
        {isEditingSelf && (
          <div className="glass-panel" style={{ height: 'fit-content', borderTop: '2px solid var(--accent-secondary)' }}>
            <h2 style={{ fontSize: '1.25rem', marginBottom: '1.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Lock size={20} color="var(--accent-secondary)" /> Security Settings
            </h2>
            
            {pwdError && <div style={{ padding: '0.75rem', background: 'rgba(239, 68, 68, 0.1)', color: 'var(--danger)', borderRadius: '8px', marginBottom: '1.5rem', fontSize: '0.875rem' }}>{pwdError}</div>}
            {pwdSuccess && <div style={{ padding: '0.75rem', background: 'rgba(16, 185, 129, 0.1)', color: 'var(--success)', borderRadius: '8px', marginBottom: '1.5rem', fontSize: '0.875rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}><CheckCircle2 size={16}/> Password updated</div>}

            <form onSubmit={handleChangePassword} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
              <div className="form-group" style={{ margin: 0 }}>
                <label className="form-label">Current Password</label>
                <input type="password" className="form-input" value={currentPassword} onChange={e => setCurrentPassword(e.target.value)} required />
              </div>
              <div className="form-group" style={{ margin: 0 }}>
                <label className="form-label">New Password</label>
                <input type="password" className="form-input" value={newPassword} onChange={e => setNewPassword(e.target.value)} required minLength={6} />
              </div>
              <div className="form-group" style={{ margin: 0 }}>
                <label className="form-label">Confirm New Password</label>
                <input type="password" className="form-input" value={confirmPassword} onChange={e => setConfirmPassword(e.target.value)} required minLength={6} />
              </div>
              <button type="submit" className="btn btn-primary" disabled={saving} style={{ marginTop: '0.5rem', background: 'var(--accent-secondary)' }}>
                <Lock size={18} /> Update Password
              </button>
            </form>
          </div>
        )}
      </div>
      
      <style>{`
        @media (min-width: 768px) {
          .animate-fade-in > div:nth-child(3) { grid-template-columns: 1fr 1fr; }
        }
      `}</style>
    </div>
  );
}
