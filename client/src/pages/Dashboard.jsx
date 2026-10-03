import { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useAccountNotificationsContext } from '../context/AccountNotificationsContext';
import { ShieldAlert, Bell, Users, Settings, Activity, CheckCircle2, User, Key, Mail, Phone, UserCircle } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { authPut } from '../utils/api';

export default function Dashboard() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const { notifications } = useAccountNotificationsContext();
  const [loadingAction, setLoadingAction] = useState(false);
  
  // Greeting logic
  const hour = new Date().getHours();
  const greeting = hour < 12 ? 'Good morning' : hour < 18 ? 'Good afternoon' : 'Good evening';

  useEffect(() => {
    if (!user) {
      navigate('/login');
    }
  }, [user, navigate]);

  const handleDisableUser = async () => {
    setLoadingAction(true);
    try {
      const res = await authPut('/api/v1/admin/users/1/status?enabled=false');
      if (!res.ok) console.error("Failed to disable user. Is Auth Service running?");
    } catch (err) {
      console.error("Network error disabling user:", err);
    } finally {
      setLoadingAction(false);
    }
  };

  if (!user) return null;

  return (
    <div className="animate-fade-in" style={{ paddingBottom: '4rem' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '2rem', flexWrap: 'wrap', gap: '1rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1.5rem' }}>
          <div style={{ width: '80px', height: '80px', borderRadius: '24px', background: 'var(--gradient-hero)', display: 'flex', alignItems: 'center', justifyContent: 'center', boxShadow: 'var(--glow-primary)' }}>
            <span style={{ fontSize: '2rem', fontWeight: 800, color: 'white' }}>{user.username.charAt(0).toUpperCase()}</span>
          </div>
          <div>
            <h1 style={{ marginBottom: '0.25rem', fontSize: '2rem' }}>{greeting}, {user.username}</h1>
            <p style={{ color: 'var(--text-secondary)', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <span style={{ display: 'inline-block', width: '8px', height: '8px', borderRadius: '50%', background: 'var(--success)', boxShadow: '0 0 8px var(--success)' }}></span>
              Session active & secured
            </p>
          </div>
        </div>
        
        <div style={{ padding: '0.5rem 1rem', background: 'rgba(255,255,255,0.05)', border: '1px solid var(--glass-border)', borderRadius: '12px', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <Key size={16} color={user.role === 'ADMIN' ? 'var(--accent-tertiary)' : 'var(--text-secondary)'} />
          <span style={{ fontWeight: 600, fontSize: '0.875rem' }}>Role: {user.role}</span>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr', gap: '2rem', '@media (min-width: 1024px)': { gridTemplateColumns: '1fr 350px' } }}>
        {/* Main Content Area */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
          
          {user.role === 'ADMIN' && (
            <div className="glass-panel" style={{ borderTop: '2px solid var(--accent-tertiary)' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '1.5rem' }}>
                <div style={{ padding: '0.75rem', background: 'rgba(34, 211, 238, 0.1)', borderRadius: '12px', color: 'var(--accent-tertiary)' }}>
                  <ShieldAlert size={24} />
                </div>
                <div>
                  <h2 style={{ fontSize: '1.5rem', margin: 0 }}>Admin Workspace</h2>
                  <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', marginTop: '0.25rem' }}>Manage platform security and user access.</p>
                </div>
              </div>
              
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem' }}>
                <button className="btn btn-primary" onClick={() => navigate('/manage-users')} style={{ justifyContent: 'center' }}>
                  <Users size={18} /> User Directory
                </button>
                <button className="btn btn-danger" onClick={handleDisableUser} disabled={loadingAction} style={{ justifyContent: 'center' }}>
                  <ShieldAlert size={18} /> {loadingAction ? 'Disabling...' : 'Test: Disable User #1'}
                </button>
              </div>
            </div>
          )}

          <div className="glass-panel">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
              <h2 style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', margin: 0 }}>
                <UserCircle color="var(--accent-primary)" /> Profile Details
              </h2>
              <button className="btn btn-primary" style={{ padding: '0.5rem 1rem', fontSize: '0.875rem' }} onClick={() => navigate('/edit-profile')}>
                <Settings size={16} /> Edit
              </button>
            </div>
            
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1.5rem' }}>
              <div style={{ background: 'rgba(0,0,0,0.2)', padding: '1rem', borderRadius: '12px', border: '1px solid var(--glass-border)' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: 'var(--text-secondary)', marginBottom: '0.5rem', fontSize: '0.875rem' }}>
                  <User size={14} /> Full Name
                </div>
                <div style={{ fontWeight: 600, fontSize: '1.125rem' }}>{user.name || user.username || 'Not provided'}</div>
              </div>
              
              <div style={{ background: 'rgba(0,0,0,0.2)', padding: '1rem', borderRadius: '12px', border: '1px solid var(--glass-border)' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: 'var(--text-secondary)', marginBottom: '0.5rem', fontSize: '0.875rem' }}>
                  <Mail size={14} /> Email Address
                </div>
                <div style={{ fontWeight: 600, fontSize: '1.125rem' }}>{user.email}</div>
              </div>
              
              <div style={{ background: 'rgba(0,0,0,0.2)', padding: '1rem', borderRadius: '12px', border: '1px solid var(--glass-border)' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: 'var(--text-secondary)', marginBottom: '0.5rem', fontSize: '0.875rem' }}>
                  <Phone size={14} /> Phone
                </div>
                <div style={{ fontWeight: 600, fontSize: '1.125rem' }}>{user.phoneNumber || 'Not provided'}</div>
              </div>

              <div style={{ background: 'rgba(0,0,0,0.2)', padding: '1rem', borderRadius: '12px', border: '1px solid var(--glass-border)' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: 'var(--text-secondary)', marginBottom: '0.5rem', fontSize: '0.875rem' }}>
                  <CheckCircle2 size={14} /> Profile Status
                </div>
                <div style={{ fontWeight: 600, fontSize: '1.125rem', color: user.profileComplete ? 'var(--success)' : 'var(--accent-secondary)' }}>
                  {user.profileComplete ? '100% Complete' : 'Action Required'}
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Sidebar Notifications */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
          <div className="glass-panel" style={{ padding: '1.5rem' }}>
            <h3 style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1.5rem', fontSize: '1.125rem' }}>
              <Activity size={20} color="var(--accent-tertiary)" /> Activity Feed
            </h3>
            
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              {notifications.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '2rem 1rem', color: 'var(--text-secondary)' }}>
                  <Bell size={24} style={{ opacity: 0.5, marginBottom: '0.5rem' }} />
                  <p style={{ fontSize: '0.875rem' }}>No recent activity</p>
                </div>
              ) : (
                notifications.map((n, idx) => {
                  const isDanger = n.type === 'ACCOUNT_DISABLED' || n.type === 'SESSION_REVOKED';
                  return (
                    <div key={n.id} className="animate-fade-in" style={{ 
                      padding: '1rem', 
                      borderRadius: '12px', 
                      background: isDanger ? 'rgba(239, 68, 68, 0.05)' : 'rgba(255, 255, 255, 0.03)',
                      border: '1px solid',
                      borderColor: isDanger ? 'rgba(239, 68, 68, 0.2)' : 'var(--glass-border)',
                      position: 'relative',
                      overflow: 'hidden',
                      animationDelay: `${idx * 0.1}s`
                    }}>
                      {isDanger && <div style={{ position: 'absolute', left: 0, top: 0, bottom: 0, width: '3px', background: 'var(--danger)' }} />}
                      {!isDanger && <div style={{ position: 'absolute', left: 0, top: 0, bottom: 0, width: '3px', background: 'var(--accent-primary)' }} />}
                      
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.25rem', fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
                        <span style={{ textTransform: 'uppercase', fontWeight: 600, color: isDanger ? 'var(--danger)' : 'var(--accent-tertiary)' }}>
                          {n.type.replace(/_/g, ' ')}
                        </span>
                        <span>•</span>
                        <span>{new Date(n.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
                      </div>
                      <p style={{ fontSize: '0.875rem', color: 'var(--text-primary)', lineHeight: 1.5 }}>
                        {n.message}
                      </p>
                    </div>
                  );
                })
              )}
            </div>
          </div>
        </div>
      </div>
      <style>{`
        @media (min-width: 1024px) {
          .glass-panel:nth-child(2) { grid-template-columns: 1fr 350px !important; }
        }
      `}</style>
    </div>
  );
}
