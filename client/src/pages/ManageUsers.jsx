import { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';
import { Users, ShieldAlert, ShieldCheck, ArrowLeft, Edit } from 'lucide-react';
import { authGet, authPut } from '../utils/api';

export default function ManageUsers() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(null);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  useEffect(() => {
    if (!user || user.role !== 'ADMIN') {
      navigate('/dashboard');
      return;
    }
    fetchUsers();
  }, [user, navigate, page]);

  const fetchUsers = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await authGet(`/api/v1/admin/users?page=${page}&size=10&sortBy=id`);
      if (!res.ok) {
        throw new Error('Failed to fetch users');
      }
      const data = await res.json();
      setUsers(data.content || []);
      setTotalPages(data.totalPages || 0);
    } catch (err) {
      setError(err.message);
      console.error('Error fetching users:', err);
    } finally {
      setLoading(false);
    }
  };

  const toggleUserStatus = async (userId, currentStatus) => {
    setActionLoading(userId);
    try {
      const res = await authPut(`/api/v1/admin/users/${userId}/status?enabled=${!currentStatus}`);
      if (!res.ok) {
        throw new Error('Failed to update user status');
      }
      // Refresh the user list
      await fetchUsers();
    } catch (err) {
      console.error('Error updating user status:', err);
      alert('Failed to update user status: ' + err.message);
    } finally {
      setActionLoading(null);
    }
  };

  if (loading && users.length === 0) {
    return (
      <div className="animate-fade-in" style={{ textAlign: 'center', padding: '3rem' }}>
        <p>Loading users...</p>
      </div>
    );
  }

  return (
    <div className="animate-fade-in">
      <div style={{ marginBottom: '2rem' }}>
        <button 
          onClick={() => navigate('/dashboard')}
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
          <ArrowLeft size={18} /> Back to Dashboard
        </button>
        
        <h1 style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
          <Users /> Manage Users
        </h1>
        <p style={{ color: 'var(--text-secondary)' }}>
          View and manage user accounts, roles, and status.
        </p>
      </div>

      {error && (
        <div className="glass-panel" style={{ 
          borderLeft: '4px solid var(--danger)', 
          marginBottom: '1.5rem',
          background: 'rgba(239, 68, 68, 0.1)'
        }}>
          <p style={{ color: 'var(--danger)' }}>Error: {error}</p>
        </div>
      )}

      <div className="glass-panel">
        <div style={{ overflowX: 'auto' }}>
          <table style={{ 
            width: '100%', 
            borderCollapse: 'collapse',
            fontSize: '0.95rem'
          }}>
            <thead>
              <tr style={{ borderBottom: '2px solid var(--glass-border)' }}>
                <th style={{ padding: '1rem', textAlign: 'left' }}>ID</th>
                <th style={{ padding: '1rem', textAlign: 'left' }}>Username</th>
                <th style={{ padding: '1rem', textAlign: 'left' }}>Email</th>
                <th style={{ padding: '1rem', textAlign: 'left' }}>Roles</th>
                <th style={{ padding: '1rem', textAlign: 'left' }}>Status</th>
                <th style={{ padding: '1rem', textAlign: 'center' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {users.map((u) => (
                <tr 
                  key={u.id} 
                  style={{ 
                    borderBottom: '1px solid var(--glass-border)',
                    opacity: !u.enabled ? 0.6 : 1
                  }}
                >
                  <td style={{ padding: '1rem' }}>{u.id}</td>
                  <td style={{ padding: '1rem', fontWeight: '500' }}>{u.username}</td>
                  <td style={{ padding: '1rem', color: 'var(--text-secondary)' }}>{u.email}</td>
                  <td style={{ padding: '1rem' }}>
                    <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                      {u.roles?.map((role, idx) => (
                        <span 
                          key={idx}
                          style={{
                            padding: '0.25rem 0.75rem',
                            background: 'rgba(99,102,241,0.2)',
                            color: 'var(--accent-primary)',
                            borderRadius: '12px',
                            fontSize: '0.75rem',
                            fontWeight: '600'
                          }}
                        >
                          {role.name}
                        </span>
                      ))}
                    </div>
                  </td>
                  <td style={{ padding: '1rem' }}>
                    <span style={{
                      padding: '0.25rem 0.75rem',
                      background: u.enabled ? 'rgba(34, 197, 94, 0.2)' : 'rgba(239, 68, 68, 0.2)',
                      color: u.enabled ? 'var(--success)' : 'var(--danger)',
                      borderRadius: '12px',
                      fontSize: '0.75rem',
                      fontWeight: '600',
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: '0.25rem'
                    }}>
                      {u.enabled ? <><ShieldCheck size={12} /> Active</> : <><ShieldAlert size={12} /> Disabled</>}
                    </span>
                  </td>
                  <td style={{ padding: '1rem' }}>
                    <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'center' }}>
                      <button
                        onClick={() => navigate(`/edit-profile/${u.id}`)}
                        className="btn btn-primary"
                        style={{ 
                          padding: '0.5rem 0.75rem',
                          fontSize: '0.875rem',
                          minWidth: 'auto'
                        }}
                        title="Edit User"
                      >
                        <Edit size={16} />
                      </button>
                      <button
                        onClick={() => toggleUserStatus(u.id, u.enabled)}
                        disabled={actionLoading === u.id}
                        className={u.enabled ? 'btn btn-danger' : 'btn btn-success'}
                        style={{ 
                          padding: '0.5rem 0.75rem',
                          fontSize: '0.875rem',
                          minWidth: '80px'
                        }}
                      >
                        {actionLoading === u.id ? (
                          'Loading...'
                        ) : u.enabled ? (
                          <>Disable</>
                        ) : (
                          <>Enable</>
                        )}
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {totalPages > 1 && (
          <div style={{ 
            display: 'flex', 
            justifyContent: 'center', 
            alignItems: 'center',
            gap: '1rem',
            marginTop: '1.5rem',
            paddingTop: '1.5rem',
            borderTop: '1px solid var(--glass-border)'
          }}>
            <button
              onClick={() => setPage(p => Math.max(0, p - 1))}
              disabled={page === 0}
              className="btn"
              style={{ minWidth: 'auto' }}
            >
              Previous
            </button>
            <span style={{ color: 'var(--text-secondary)' }}>
              Page {page + 1} of {totalPages}
            </span>
            <button
              onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
              disabled={page >= totalPages - 1}
              className="btn"
              style={{ minWidth: 'auto' }}
            >
              Next
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
