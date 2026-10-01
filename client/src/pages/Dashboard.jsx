import { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { ShieldAlert, Bell, Users, Settings } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { authPut } from '../utils/api';

export default function Dashboard() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [notifications, setNotifications] = useState([]);
  const [loadingAction, setLoadingAction] = useState(false);

  useEffect(() => {
    if (!user) {
      navigate('/login');
      return;
    }

    // Connect to real SSE endpoint using cookie-based authentication
    // The refreshToken cookie will be sent automatically with withCredentials
    const userId = user.id || 1; 
    const eventSource = new EventSource(`/api/v1/notifications/stream?userId=${userId}`, {
      withCredentials: true
    });

    eventSource.onopen = () => {
      console.log('SSE connection opened.');
    };

    eventSource.addEventListener('CONNECTED', (e) => {
      setNotifications(prev => [{ id: Date.now(), msg: e.data, type: 'info' }, ...prev]);
    });

    eventSource.addEventListener('ACCOUNT_DISABLED', (e) => {
      setNotifications(prev => [{ id: Date.now(), msg: e.data, type: 'danger' }, ...prev]);
    });

    eventSource.onerror = (e) => {
      console.error('SSE Error:', e);
      eventSource.close();
    };

    return () => {
      eventSource.close();
    };
  }, [user, navigate]);

  const handleDisableUser = async () => {
    setLoadingAction(true);
    try {
      // Call the existing AdminController endpoint to disable user ID 1
      const res = await authPut('/api/v1/admin/users/1/status?enabled=false');
      if (!res.ok) {
        console.error("Failed to disable user. Is Auth Service running?");
      }
    } catch (err) {
      console.error("Network error disabling user:", err);
    } finally {
      setLoadingAction(false);
    }
  };

  if (!user) return null;

  return (
    <div className="animate-fade-in">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
        <div>
          <h1 style={{ marginBottom: '0.5rem' }}>Dashboard</h1>
          <p style={{ color: 'var(--text-secondary)' }}>Welcome back, {user.username}!</p>
        </div>
        <div style={{ padding: '0.5rem 1rem', background: 'rgba(99,102,241,0.1)', color: 'var(--accent-primary)', borderRadius: '8px', fontWeight: 'bold' }}>
          Role: {user.role}
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 350px', gap: '2rem' }}>
        {/* Main Content Area */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          
          {user.role === 'ADMIN' ? (
            <div className="glass-panel" style={{ borderLeft: '4px solid var(--accent-primary)' }}>
              <h2 style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '1.5rem' }}>
                <ShieldAlert /> Admin Controls
              </h2>
              <p style={{ color: 'var(--text-secondary)', marginBottom: '1.5rem' }}>
                You have administrative privileges. You can manage roles and disable user accounts.
              </p>
              
              <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap' }}>
                <button 
                  className="btn btn-primary"
                  onClick={() => navigate('/manage-users')}
                >
                  <Users size={18} /> Manage Users
                </button>
                <button 
                  className="btn btn-primary" 
                  onClick={() => navigate('/edit-profile')}
                  style={{ background: 'var(--success)' }}
                >
                  <Settings size={18} /> Edit Profile
                </button>
                <button 
                  className="btn btn-danger" 
                  onClick={handleDisableUser}
                  disabled={loadingAction}
                >
                  <ShieldAlert size={18} /> {loadingAction ? 'Disabling...' : 'Test: Disable User ID 1'}
                </button>
              </div>
            </div>
          ) : (
            <div className="glass-panel" style={{ borderLeft: '4px solid var(--success)' }}>
              <h2 style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '1.5rem' }}>
                <Settings /> User Settings
              </h2>
              <p style={{ color: 'var(--text-secondary)', marginBottom: '1.5rem' }}>
                Manage your personal profile and preferences.
              </p>
              <button 
                className="btn btn-primary" 
                onClick={() => navigate('/edit-profile')}
                style={{ background: 'var(--success)' }}
              >
                Edit Profile
              </button>
            </div>
          )}

          <div className="glass-panel">
            <h3 style={{ marginBottom: '1.5rem', borderBottom: '1px solid var(--glass-border)', paddingBottom: '0.5rem' }}>Profile Overview</h3>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
              <div>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', marginBottom: '0.25rem' }}>Full Name</p>
                <p style={{ fontWeight: '500' }}>{user.name || user.username || 'Not provided'}</p>
              </div>
              <div>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', marginBottom: '0.25rem' }}>Email Address</p>
                <p style={{ fontWeight: '500' }}>{user.email}</p>
              </div>
              <div>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', marginBottom: '0.25rem' }}>Phone Number</p>
                <p style={{ fontWeight: '500' }}>{user.phoneNumber || 'Not provided'}</p>
              </div>
              <div>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', marginBottom: '0.25rem' }}>Gender</p>
                <p style={{ fontWeight: '500', textTransform: 'capitalize' }}>{user.gender?.toLowerCase() || 'Not provided'}</p>
              </div>
              <div>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', marginBottom: '0.25rem' }}>Role</p>
                <p style={{ fontWeight: '500' }}>{user.role}</p>
              </div>
              <div>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', marginBottom: '0.25rem' }}>Profile Status</p>
                <p style={{ fontWeight: '500', color: user.profileComplete ? 'var(--success)' : 'var(--warning)' }}>
                  {user.profileComplete ? 'Complete' : 'Incomplete'}
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* Sidebar Notifications */}
        <div className="glass-panel" style={{ height: 'fit-content' }}>
          <h3 style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', borderBottom: '1px solid var(--glass-border)', paddingBottom: '1rem', marginBottom: '1rem' }}>
            <Bell /> Real-time Notifications
          </h3>
          
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            {notifications.length === 0 ? (
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>No new notifications.</p>
            ) : (
              notifications.map(n => (
                <div key={n.id} className="animate-fade-in" style={{ 
                  padding: '1rem', 
                  borderRadius: '8px', 
                  background: n.type === 'danger' ? 'rgba(239, 68, 68, 0.1)' : 'rgba(99, 102, 241, 0.1)',
                  borderLeft: `3px solid ${n.type === 'danger' ? 'var(--danger)' : 'var(--accent-primary)'}`
                }}>
                  <p style={{ fontSize: '0.875rem', color: n.type === 'danger' ? '#fca5a5' : 'var(--text-primary)' }}>
                    {n.msg}
                  </p>
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
