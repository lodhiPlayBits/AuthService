import { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useNavigate, useParams } from 'react-router-dom';
import { User, Mail, ArrowLeft, Save, Lock } from 'lucide-react';
import { authGet, authPut, authPost } from '../utils/api';

export default function EditProfile() {
  const { user, updateUser } = useAuth();
  const navigate = useNavigate();
  const { userId } = useParams(); // Optional: if editing another user (admin)
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [userData, setUserData] = useState(null);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(false);
  
  // Form state
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [name, setName] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [gender, setGender] = useState('MALE');
  
  // Password change state
  const [showPasswordChange, setShowPasswordChange] = useState(false);
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');

  const targetUserId = userId || user?.id;
  const isEditingSelf = !userId || userId == user?.id;
  const canEdit = isEditingSelf || user?.role === 'ADMIN';

  useEffect(() => {
    if (!user) {
      navigate('/login');
      return;
    }
    if (!targetUserId) {
      console.error('No user ID available');
      setError('User ID not found. Please log in again.');
      setLoading(false);
      return;
    }
    if (!canEdit) {
      navigate('/dashboard');
      return;
    }
    fetchUserData();
  }, [user, targetUserId, navigate]);

  const fetchUserData = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await authGet(`/api/v1/users/${targetUserId}`);
      if (!res.ok) {
        throw new Error('Failed to fetch user data');
      }
      const data = await res.json();
      setUserData(data);
      setUsername(data.username || '');
      setEmail(data.email || '');
      setName(data.name || '');
      setPhoneNumber(data.phoneNumber || '');
      setGender(data.gender || 'MALE');
    } catch (err) {
      setError(err.message);
      console.error('Error fetching user data:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleSaveProfile = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError(null);
    setSuccess(false);

    try {
      const payload = { username, email, name, phoneNumber, gender };
      const res = await authPut(`/api/v1/users/${targetUserId}`, payload);

      if (!res.ok) {
        const errorData = await res.json().catch(() => null);
        throw new Error(errorData?.message || 'Failed to update profile');
      }

      setSuccess(true);
      
      // Update global context if editing self
      if (isEditingSelf) {
        updateUser(payload);
      }
      
      await fetchUserData(); // Refresh data
      
      // Auto-hide success message after 3 seconds
      setTimeout(() => setSuccess(false), 3000);
    } catch (err) {
      setError(err.message);
      console.error('Error updating profile:', err);
    } finally {
      setSaving(false);
    }
  };

  const handleChangePassword = async (e) => {
    e.preventDefault();
    
    if (newPassword !== confirmPassword) {
      setError('New passwords do not match');
      return;
    }

    if (newPassword.length < 6) {
      setError('Password must be at least 6 characters');
      return;
    }

    setSaving(true);
    setError(null);
    setSuccess(false);

    try {
      const res = await authPost(`/api/v1/users/${targetUserId}/change-password`, {
        currentPassword, 
        newPassword 
      });

      if (!res.ok) {
        const errorData = await res.json().catch(() => null);
        throw new Error(errorData?.message || 'Failed to change password');
      }

      setSuccess(true);
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
      setShowPasswordChange(false);
      
      // Auto-hide success message after 3 seconds
      setTimeout(() => setSuccess(false), 3000);
    } catch (err) {
      setError(err.message);
      console.error('Error changing password:', err);
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="animate-fade-in" style={{ textAlign: 'center', padding: '3rem' }}>
        <p>Loading profile...</p>
      </div>
    );
  }

  return (
    <div className="animate-fade-in">
      <div style={{ marginBottom: '2rem' }}>
        <button 
          onClick={() => navigate(userId ? '/manage-users' : '/dashboard')}
          className="btn"
          style={{ 
            background: 'transparent', 
            border: '1px solid var(--glass-border)',
            marginBottom: '1rem',
            display: 'inline-flex',
            alignItems: 'center',
            gap: '0.5rem'
          }}
        >
          <ArrowLeft size={18} /> Back
        </button>
        
        <h1 style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
          <User /> {isEditingSelf ? 'Edit Your Profile' : `Edit User: ${userData?.username}`}
        </h1>
        <p style={{ color: 'var(--text-secondary)' }}>
          Update account information and settings.
        </p>
      </div>

      {error && (
        <div className="glass-panel animate-fade-in" style={{ 
          borderLeft: '4px solid var(--danger)', 
          marginBottom: '1.5rem',
          background: 'rgba(239, 68, 68, 0.1)'
        }}>
          <p style={{ color: 'var(--danger)' }}>{error}</p>
        </div>
      )}

      {success && (
        <div className="glass-panel animate-fade-in" style={{ 
          borderLeft: '4px solid var(--success)', 
          marginBottom: '1.5rem',
          background: 'rgba(34, 197, 94, 0.1)'
        }}>
          <p style={{ color: 'var(--success)' }}>Changes saved successfully!</p>
        </div>
      )}

      {/* Profile Information Form */}
      <div className="glass-panel" style={{ marginBottom: '1.5rem' }}>
        <h2 style={{ marginBottom: '1.5rem' }}>Profile Information</h2>
        
        <form onSubmit={handleSaveProfile}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
            <div>
              <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: '500' }}>
                <User size={16} style={{ display: 'inline', marginRight: '0.5rem' }} />
                Full Name
              </label>
              <input
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
                style={{
                  width: '100%',
                  padding: '0.75rem',
                  background: 'var(--glass-bg)',
                  border: '1px solid var(--glass-border)',
                  borderRadius: '8px',
                  color: 'var(--text-primary)',
                  fontSize: '1rem'
                }}
              />
            </div>

            <div>
              <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: '500' }}>
                <User size={16} style={{ display: 'inline', marginRight: '0.5rem' }} />
                Username
              </label>
              <input
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
                style={{
                  width: '100%',
                  padding: '0.75rem',
                  background: 'var(--glass-bg)',
                  border: '1px solid var(--glass-border)',
                  borderRadius: '8px',
                  color: 'var(--text-primary)',
                  fontSize: '1rem'
                }}
              />
            </div>

            <div>
              <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: '500' }}>
                <Mail size={16} style={{ display: 'inline', marginRight: '0.5rem' }} />
                Email
              </label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                style={{
                  width: '100%',
                  padding: '0.75rem',
                  background: 'var(--glass-bg)',
                  border: '1px solid var(--glass-border)',
                  borderRadius: '8px',
                  color: 'var(--text-primary)',
                  fontSize: '1rem'
                }}
              />
            </div>

            <div>
              <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: '500' }}>
                Phone Number
              </label>
              <input
                type="tel"
                value={phoneNumber}
                onChange={(e) => setPhoneNumber(e.target.value)}
                required
                pattern="[0-9]{10}"
                title="Must be a 10 digit number"
                style={{
                  width: '100%',
                  padding: '0.75rem',
                  background: 'var(--glass-bg)',
                  border: '1px solid var(--glass-border)',
                  borderRadius: '8px',
                  color: 'var(--text-primary)',
                  fontSize: '1rem'
                }}
              />
            </div>

            <div>
              <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: '500' }}>
                Gender
              </label>
              <select
                value={gender}
                onChange={(e) => setGender(e.target.value)}
                required
                style={{
                  width: '100%',
                  padding: '0.75rem',
                  background: 'var(--glass-bg)',
                  border: '1px solid var(--glass-border)',
                  borderRadius: '8px',
                  color: 'var(--text-primary)',
                  fontSize: '1rem'
                }}
              >
                <option value="MALE">Male</option>
                <option value="FEMALE">Female</option>
                <option value="OTHER">Other</option>
              </select>
            </div>

            <div style={{ display: 'flex', gap: '1rem' }}>
              <button 
                type="submit" 
                className="btn btn-primary"
                disabled={saving}
                style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}
              >
                <Save size={18} /> {saving ? 'Saving...' : 'Save Changes'}
              </button>
            </div>
          </div>
        </form>
      </div>

      {/* Password Change Section (only for editing own profile) */}
      {isEditingSelf && (
        <div className="glass-panel">
          <h2 style={{ marginBottom: '1rem' }}>Change Password</h2>
          
          {!showPasswordChange ? (
            <button 
              onClick={() => setShowPasswordChange(true)}
              className="btn"
              style={{ 
                background: 'transparent',
                border: '1px solid var(--glass-border)',
                display: 'flex',
                alignItems: 'center',
                gap: '0.5rem'
              }}
            >
              <Lock size={18} /> Change Password
            </button>
          ) : (
            <form onSubmit={handleChangePassword}>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
                <div>
                  <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: '500' }}>
                    Current Password
                  </label>
                  <input
                    type="password"
                    value={currentPassword}
                    onChange={(e) => setCurrentPassword(e.target.value)}
                    required
                    style={{
                      width: '100%',
                      padding: '0.75rem',
                      background: 'var(--glass-bg)',
                      border: '1px solid var(--glass-border)',
                      borderRadius: '8px',
                      color: 'var(--text-primary)',
                      fontSize: '1rem'
                    }}
                  />
                </div>

                <div>
                  <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: '500' }}>
                    New Password
                  </label>
                  <input
                    type="password"
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    required
                    minLength={6}
                    style={{
                      width: '100%',
                      padding: '0.75rem',
                      background: 'var(--glass-bg)',
                      border: '1px solid var(--glass-border)',
                      borderRadius: '8px',
                      color: 'var(--text-primary)',
                      fontSize: '1rem'
                    }}
                  />
                </div>

                <div>
                  <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: '500' }}>
                    Confirm New Password
                  </label>
                  <input
                    type="password"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    required
                    minLength={6}
                    style={{
                      width: '100%',
                      padding: '0.75rem',
                      background: 'var(--glass-bg)',
                      border: '1px solid var(--glass-border)',
                      borderRadius: '8px',
                      color: 'var(--text-primary)',
                      fontSize: '1rem'
                    }}
                  />
                </div>

                <div style={{ display: 'flex', gap: '1rem' }}>
                  <button 
                    type="submit" 
                    className="btn btn-primary"
                    disabled={saving}
                  >
                    {saving ? 'Updating...' : 'Update Password'}
                  </button>
                  <button 
                    type="button"
                    onClick={() => {
                      setShowPasswordChange(false);
                      setCurrentPassword('');
                      setNewPassword('');
                      setConfirmPassword('');
                      setError(null);
                    }}
                    className="btn"
                    style={{ 
                      background: 'transparent',
                      border: '1px solid var(--glass-border)'
                    }}
                  >
                    Cancel
                  </button>
                </div>
              </div>
            </form>
          )}
        </div>
      )}
    </div>
  );
}
