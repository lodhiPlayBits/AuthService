import { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useAnnouncementsContext } from '../context/AnnouncementsContext';
import { useNavigate } from 'react-router-dom';
import { Users, ShieldAlert, ShieldCheck, ArrowLeft, Edit, Megaphone, Send, BellRing, Search, SearchX } from 'lucide-react';
import { authGet, authPut } from '../utils/api';

export default function ManageUsers() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const { sendAnnouncement, adminResponses, errors } = useAnnouncementsContext();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(null);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [refreshKey, setRefreshKey] = useState(0);
  const [announcementForm, setAnnouncementForm] = useState({ title: '', message: '', priority: 'NORMAL' });

  useEffect(() => {
    if (!user || user.role !== 'ADMIN') {
      navigate('/dashboard'); return;
    }
    let cancelled = false;
    authGet(`/api/v1/admin/users?page=${page}&size=10&sortBy=id`)
      .then(res => { if (!res.ok) throw new Error('Failed to fetch users'); return res.json(); })
      .then(data => {
        if (cancelled) return;
        setUsers(data.content || []); setTotalPages(data.totalPages || 0); setError(null);
      })
      .catch(err => { if (!cancelled) setError(err.message); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [user, navigate, page, refreshKey]);

  const toggleUserStatus = async (userId, currentStatus) => {
    setActionLoading(userId);
    try {
      const res = await authPut(`/api/v1/admin/users/${userId}/status?enabled=${!currentStatus}`);
      if (!res.ok) throw new Error('Failed to update user status');
      setRefreshKey(key => key + 1);
    } catch (err) {
      alert('Failed to update user status: ' + err.message);
    } finally {
      setActionLoading(null);
    }
  };

  const handleSendAnnouncement = (e) => {
    e.preventDefault();
    const { title, message, priority } = announcementForm;
    if (!title.trim() || !message.trim()) return alert('Title and message are required');
    if (!sendAnnouncement(title.trim(), message.trim(), priority)) return alert('Connection not ready. Try again.');
    setAnnouncementForm({ title: '', message: '', priority: 'NORMAL' });
  };

  if (loading && users.length === 0) {
    return <div style={{ display: 'flex', justifyContent: 'center', padding: '4rem' }}><div className="spin"><Search size={32} color="var(--text-secondary)" /></div></div>;
  }

  return (
    <div className="animate-fade-in" style={{ paddingBottom: '4rem' }}>
      <div style={{ marginBottom: '2rem' }}>
        <button onClick={() => navigate('/dashboard')} className="btn" style={{ background: 'transparent', border: '1px solid var(--glass-border)', padding: '0.5rem 1rem', fontSize: '0.875rem', marginBottom: '1.5rem' }}>
          <ArrowLeft size={16} /> Dashboard
        </button>
        <h1 style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', margin: 0 }}>
          <div style={{ background: 'var(--gradient-hero)', padding: '0.5rem', borderRadius: '12px' }}><Users color="white" size={24} /></div>
          User Directory
        </h1>
        <p style={{ color: 'var(--text-secondary)', marginTop: '0.5rem' }}>Manage accounts, monitor statuses, and broadcast events.</p>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr', gap: '2rem', marginBottom: '2rem', '@media (min-width: 1024px)': { gridTemplateColumns: '2fr 1fr' } }}>
        <div className="glass-panel" style={{ borderTop: '2px solid var(--accent-tertiary)' }}>
          <h2 style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '1.25rem', marginBottom: '1.5rem' }}>
            <Megaphone size={20} color="var(--accent-tertiary)" /> Broadcast Announcement
          </h2>
          <form onSubmit={handleSendAnnouncement} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            <input type="text" className="form-input" placeholder="Announcement Title" maxLength={100} value={announcementForm.title} onChange={e => setAnnouncementForm({...announcementForm, title: e.target.value})} required />
            <textarea className="form-input" placeholder="Message content..." rows={3} maxLength={2000} value={announcementForm.message} onChange={e => setAnnouncementForm({...announcementForm, message: e.target.value})} required style={{ resize: 'vertical' }} />
            <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap' }}>
              <select className="form-input" style={{ flex: 1, minWidth: '150px' }} value={announcementForm.priority} onChange={e => setAnnouncementForm({...announcementForm, priority: e.target.value})}>
                <option value="LOW">Low Priority</option><option value="NORMAL">Normal Priority</option><option value="HIGH">High Priority</option>
              </select>
              <button type="submit" className="btn btn-primary" style={{ flex: 1, minWidth: '150px' }}><Send size={16} /> Broadcast</button>
            </div>
          </form>
          {errors.length > 0 && <div style={{ marginTop: '1rem', padding: '0.75rem', background: 'rgba(239, 68, 68, 0.1)', color: 'var(--danger)', borderRadius: '8px', fontSize: '0.875rem' }}>{errors[0].message}</div>}
        </div>

        <div className="glass-panel" style={{ overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
          <h2 style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '1.25rem', marginBottom: '1.5rem' }}>
            <BellRing size={20} color="var(--accent-secondary)" /> Live Responses
          </h2>
          <div style={{ flex: 1, overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            {adminResponses.length === 0 ? (
              <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: '100%', color: 'var(--text-secondary)', opacity: 0.5 }}>
                <SearchX size={32} style={{ marginBottom: '0.5rem' }} />
                <span style={{ fontSize: '0.875rem' }}>Awaiting responses...</span>
              </div>
            ) : (
              adminResponses.slice(0, 5).map((r, i) => (
                <div key={i} className="animate-fade-in" style={{ padding: '0.75rem', background: 'rgba(255,255,255,0.03)', borderRadius: '8px', borderLeft: `2px solid ${r.responseAction === 'ACK' ? 'var(--success)' : 'var(--text-secondary)'}`, fontSize: '0.875rem' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.25rem' }}>
                    <span style={{ fontWeight: 600 }}>User {r.userId}</span>
                    <span style={{ color: 'var(--text-secondary)', fontSize: '0.75rem' }}>{new Date(r.timestamp).toLocaleTimeString()}</span>
                  </div>
                  <span style={{ color: r.responseAction === 'ACK' ? 'var(--success)' : 'var(--text-secondary)' }}>
                    {r.responseAction === 'ACK' ? 'Acknowledged' : 'Dismissed'} ID: {r.announcementId}
                  </span>
                </div>
              ))
            )}
          </div>
        </div>
      </div>

      {error && <div className="glass-panel" style={{ background: 'rgba(239, 68, 68, 0.1)', border: '1px solid var(--danger)', color: 'var(--danger)', marginBottom: '2rem' }}>{error}</div>}

      <div className="glass-panel" style={{ padding: 0, overflow: 'hidden' }}>
        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
            <thead>
              <tr style={{ background: 'rgba(255,255,255,0.02)', borderBottom: '1px solid var(--glass-border)' }}>
                <th style={{ padding: '1.25rem 1.5rem', fontWeight: 600, color: 'var(--text-secondary)', fontSize: '0.875rem' }}>User</th>
                <th style={{ padding: '1.25rem 1.5rem', fontWeight: 600, color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Roles</th>
                <th style={{ padding: '1.25rem 1.5rem', fontWeight: 600, color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Status</th>
                <th style={{ padding: '1.25rem 1.5rem', fontWeight: 600, color: 'var(--text-secondary)', fontSize: '0.875rem', textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.id} style={{ borderBottom: '1px solid var(--glass-border)', transition: 'background 0.2s' }} onMouseEnter={e => e.currentTarget.style.background = 'rgba(255,255,255,0.02)'} onMouseLeave={e => e.currentTarget.style.background = 'transparent'}>
                  <td style={{ padding: '1.25rem 1.5rem' }}>
                    <div style={{ fontWeight: 500, color: 'var(--text-primary)' }}>{u.username}</div>
                    <div style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>{u.email}</div>
                  </td>
                  <td style={{ padding: '1.25rem 1.5rem' }}>
                    <div style={{ display: 'flex', gap: '0.5rem' }}>
                      {u.roles?.map((r, i) => (
                        <span key={i} style={{ padding: '0.25rem 0.75rem', background: 'rgba(129, 140, 248, 0.1)', color: 'var(--accent-primary)', borderRadius: '999px', fontSize: '0.75rem', fontWeight: 600 }}>{r.name}</span>
                      ))}
                    </div>
                  </td>
                  <td style={{ padding: '1.25rem 1.5rem' }}>
                    <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.375rem', padding: '0.25rem 0.75rem', borderRadius: '999px', fontSize: '0.75rem', fontWeight: 600, background: u.enabled ? 'rgba(16, 185, 129, 0.1)' : 'rgba(239, 68, 68, 0.1)', color: u.enabled ? 'var(--success)' : 'var(--danger)' }}>
                      {u.enabled ? <><ShieldCheck size={14} /> Active</> : <><ShieldAlert size={14} /> Disabled</>}
                    </span>
                  </td>
                  <td style={{ padding: '1.25rem 1.5rem', textAlign: 'right' }}>
                    <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'flex-end' }}>
                      <button onClick={() => navigate(`/edit-profile/${u.id}`)} className="btn" style={{ padding: '0.5rem', background: 'rgba(255,255,255,0.05)', border: '1px solid var(--glass-border)' }} title="Edit User">
                        <Edit size={16} />
                      </button>
                      <button onClick={() => toggleUserStatus(u.id, u.enabled)} disabled={actionLoading === u.id} className={u.enabled ? 'btn btn-danger' : 'btn btn-success'} style={{ padding: '0.5rem 1rem', minWidth: '100px', fontSize: '0.875rem' }}>
                        {actionLoading === u.id ? 'Loading...' : u.enabled ? 'Disable' : 'Enable'}
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        
        {totalPages > 1 && (
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '1.25rem 1.5rem', background: 'rgba(255,255,255,0.02)', borderTop: '1px solid var(--glass-border)' }}>
            <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0} className="btn" style={{ padding: '0.5rem 1rem', fontSize: '0.875rem', background: 'rgba(255,255,255,0.05)' }}>Prev</button>
            <span style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>Page <span style={{ color: 'var(--text-primary)', fontWeight: 600 }}>{page + 1}</span> of {totalPages}</span>
            <button onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))} disabled={page >= totalPages - 1} className="btn" style={{ padding: '0.5rem 1rem', fontSize: '0.875rem', background: 'rgba(255,255,255,0.05)' }}>Next</button>
          </div>
        )}
      </div>
      
      <style>{`
        @media (min-width: 1024px) {
          .glass-panel:nth-child(2) > div { flex-direction: row !important; }
        }
      `}</style>
    </div>
  );
}
