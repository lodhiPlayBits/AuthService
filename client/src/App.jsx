import React, { useState } from 'react';

const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api/v1';

// ── Reusable Components ──────────────────────────────────────────

const ResponsePanel = ({ response, loading }) => {
  if (loading) return <div className="response-panel"><div className="spinner"></div></div>;
  if (!response) return null;
  return (
    <div className={`response-panel ${response.ok ? 'response-success' : 'response-error'}`}>
      <div className="response-status">
        <span className="status-badge" data-ok={response.ok}>
          {response.status} {response.statusText}
        </span>
        <span className="response-time">{response.time}ms</span>
      </div>
      <pre className="response-body">{JSON.stringify(response.data, null, 2)}</pre>
    </div>
  );
};

const EndpointCard = ({ method, path, description, children, onSend }) => (
  <div className="endpoint-card">
    <div className="endpoint-header">
      <span className={`method-badge method-${method.toLowerCase()}`}>{method}</span>
      <code className="endpoint-path">{path}</code>
    </div>
    <p className="endpoint-desc">{description}</p>
    <div className="endpoint-body">{children}</div>
  </div>
);

// ── API Helper ──────────────────────────────────────────────────

async function apiCall(method, path, body, token) {
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const start = performance.now();
  try {
    const res = await fetch(`${API_BASE}${path}`, {
      method,
      headers,
      body: body ? JSON.stringify(body) : undefined,
      credentials: 'include', // for cookies (refresh token, CSRF)
    });
    const time = Math.round(performance.now() - start);
    let data;
    const text = await res.text();
    try { data = JSON.parse(text); } catch { data = text || '(empty body)'; }
    return { ok: res.ok, status: res.status, statusText: res.statusText, data, time };
  } catch (err) {
    return { ok: false, status: 0, statusText: 'Network Error', data: err.message, time: Math.round(performance.now() - start) };
  }
}

// ── Main App ────────────────────────────────────────────────────

function App() {
  const [token, setToken] = useState(localStorage.getItem('authToken') || '');
  const [activeTab, setActiveTab] = useState('auth');

  const saveToken = (t) => { setToken(t); localStorage.setItem('authToken', t); };
  const clearToken = () => { setToken(''); localStorage.removeItem('authToken'); };

  const tabs = [
    { id: 'auth', label: 'Auth', icon: '🔐' },
    { id: 'users', label: 'Users', icon: '👤' },
    { id: 'admin', label: 'Admin', icon: '⚙️' },
  ];

  return (
    <div className="app-container">
      <header className="app-header">
        <h1>Auth Service API Tester</h1>
        <p className="subtitle">Comprehensive endpoint testing client</p>
      </header>

      {/* Token Bar */}
      <div className="token-bar glass-container">
        <label>Bearer Token</label>
        <div className="token-input-row">
          <input
            type="text"
            className="form-input token-input"
            placeholder="Paste or login to auto-fill..."
            value={token}
            onChange={(e) => saveToken(e.target.value)}
          />
          <button className="btn btn-small btn-danger" onClick={clearToken}>Clear</button>
        </div>
      </div>

      {/* Tabs */}
      <nav className="tab-nav">
        {tabs.map(t => (
          <button
            key={t.id}
            className={`tab-btn ${activeTab === t.id ? 'active' : ''}`}
            onClick={() => setActiveTab(t.id)}
          >
            <span className="tab-icon">{t.icon}</span> {t.label}
          </button>
        ))}
      </nav>

      {/* Tab Content */}
      <div className="tab-content">
        {activeTab === 'auth' && <AuthTab token={token} onToken={saveToken} />}
        {activeTab === 'users' && <UsersTab token={token} />}
        {activeTab === 'admin' && <AdminTab token={token} />}
      </div>
    </div>
  );
}

// ══════════════════════════════════════════════════════════════════
//  AUTH TAB
// ══════════════════════════════════════════════════════════════════

function AuthTab({ token, onToken }) {
  // ── Register ──
  const [regForm, setRegForm] = useState({ name: '', email: '', username: '', password: '', phoneNumber: '', gender: 'MALE' });
  const [regRes, setRegRes] = useState(null);
  const [regLoading, setRegLoading] = useState(false);

  const doRegister = async () => {
    setRegLoading(true);
    const res = await apiCall('POST', '/auth/register', regForm);
    setRegRes(res);
    setRegLoading(false);
  };

  // ── Login ──
  const [loginForm, setLoginForm] = useState({ identifier: '', password: '' });
  const [loginRes, setLoginRes] = useState(null);
  const [loginLoading, setLoginLoading] = useState(false);

  const doLogin = async () => {
    setLoginLoading(true);
    const res = await apiCall('POST', '/auth/login', loginForm);
    setLoginRes(res);
    if (res.ok && res.data?.accessToken) onToken(res.data.accessToken);
    setLoginLoading(false);
  };

  // ── Refresh ──
  const [refreshRes, setRefreshRes] = useState(null);
  const [refreshLoading, setRefreshLoading] = useState(false);
  const doRefresh = async () => {
    setRefreshLoading(true);
    const res = await apiCall('POST', '/auth/refresh', null, token);
    setRefreshRes(res);
    if (res.ok && res.data?.accessToken) onToken(res.data.accessToken);
    setRefreshLoading(false);
  };

  // ── Logout ──
  const [logoutRes, setLogoutRes] = useState(null);
  const [logoutLoading, setLogoutLoading] = useState(false);
  const doLogout = async () => {
    setLogoutLoading(true);
    const res = await apiCall('POST', '/auth/logout', null, token);
    setLogoutRes(res);
    setLogoutLoading(false);
  };

  // ── Logout All ──
  const [logoutAllRes, setLogoutAllRes] = useState(null);
  const [logoutAllLoading, setLogoutAllLoading] = useState(false);
  const doLogoutAll = async () => {
    setLogoutAllLoading(true);
    const res = await apiCall('POST', '/auth/logout/all', null, token);
    setLogoutAllRes(res);
    setLogoutAllLoading(false);
  };

  return (
    <div className="endpoints-grid">
      {/* REGISTER */}
      <EndpointCard method="POST" path="/api/v1/auth/register" description="Register a new user account (public)">
        <div className="form-row">
          <input className="form-input" placeholder="Name" value={regForm.name} onChange={e => setRegForm({...regForm, name: e.target.value})} />
          <input className="form-input" placeholder="Username" value={regForm.username} onChange={e => setRegForm({...regForm, username: e.target.value})} />
        </div>
        <div className="form-row">
          <input className="form-input" placeholder="Email" value={regForm.email} onChange={e => setRegForm({...regForm, email: e.target.value})} />
          <input className="form-input" placeholder="Phone (10 digits)" value={regForm.phoneNumber} onChange={e => setRegForm({...regForm, phoneNumber: e.target.value})} />
        </div>
        <div className="form-row">
          <input className="form-input" type="password" placeholder="Password (min 6 chars)" value={regForm.password} onChange={e => setRegForm({...regForm, password: e.target.value})} />
          <select className="form-input" value={regForm.gender} onChange={e => setRegForm({...regForm, gender: e.target.value})}>
            <option value="MALE">MALE</option><option value="FEMALE">FEMALE</option><option value="OTHER">OTHER</option>
          </select>
        </div>
        <button className="btn" onClick={doRegister} disabled={regLoading}>{regLoading ? <div className="spinner"/> : 'Send Request'}</button>
        <ResponsePanel response={regRes} loading={regLoading} />
      </EndpointCard>

      {/* LOGIN */}
      <EndpointCard method="POST" path="/api/v1/auth/login" description="Login with identifier (username/email/phone) + password (public)">
        <input className="form-input" placeholder="Identifier (username, email, or phone)" value={loginForm.identifier} onChange={e => setLoginForm({...loginForm, identifier: e.target.value})} />
        <input className="form-input" type="password" placeholder="Password" value={loginForm.password} onChange={e => setLoginForm({...loginForm, password: e.target.value})} />
        <button className="btn" onClick={doLogin} disabled={loginLoading}>{loginLoading ? <div className="spinner"/> : 'Send Request'}</button>
        <ResponsePanel response={loginRes} loading={loginLoading} />
      </EndpointCard>

      {/* REFRESH */}
      <EndpointCard method="POST" path="/api/v1/auth/refresh" description="Refresh access token using refresh token cookie (requires cookie)">
        <button className="btn" onClick={doRefresh} disabled={refreshLoading}>{refreshLoading ? <div className="spinner"/> : 'Send Request'}</button>
        <ResponsePanel response={refreshRes} loading={refreshLoading} />
      </EndpointCard>

      {/* LOGOUT */}
      <EndpointCard method="POST" path="/api/v1/auth/logout" description="Logout current session (revokes refresh token family)">
        <button className="btn" onClick={doLogout} disabled={logoutLoading}>{logoutLoading ? <div className="spinner"/> : 'Send Request'}</button>
        <ResponsePanel response={logoutRes} loading={logoutLoading} />
      </EndpointCard>

      {/* LOGOUT ALL */}
      <EndpointCard method="POST" path="/api/v1/auth/logout/all" description="Logout from all devices (requires Bearer token)">
        <button className="btn" onClick={doLogoutAll} disabled={logoutAllLoading}>{logoutAllLoading ? <div className="spinner"/> : 'Send Request'}</button>
        <ResponsePanel response={logoutAllRes} loading={logoutAllLoading} />
      </EndpointCard>
    </div>
  );
}

// ══════════════════════════════════════════════════════════════════
//  USERS TAB
// ══════════════════════════════════════════════════════════════════

function UsersTab({ token }) {
  // ── Get User by ID ──
  const [getUserId, setGetUserId] = useState('');
  const [getUserRes, setGetUserRes] = useState(null);
  const [getUserLoading, setGetUserLoading] = useState(false);
  const doGetUser = async () => {
    setGetUserLoading(true);
    setGetUserRes(await apiCall('GET', `/users/${getUserId}`, null, token));
    setGetUserLoading(false);
  };

  // ── Get User by Email ──
  const [getEmailForm, setGetEmailForm] = useState({ email: '' });
  const [getEmailRes, setGetEmailRes] = useState(null);
  const [getEmailLoading, setGetEmailLoading] = useState(false);
  const doGetEmail = async () => {
    setGetEmailLoading(true);
    setGetEmailRes(await apiCall('GET', '/users/getByEmail', getEmailForm, token));
    setGetEmailLoading(false);
  };

  // ── Update User ──
  const [updateId, setUpdateId] = useState('');
  const [updateForm, setUpdateForm] = useState({ name: '', gender: 'MALE', image: '' });
  const [updateRes, setUpdateRes] = useState(null);
  const [updateLoading, setUpdateLoading] = useState(false);
  const doUpdate = async () => {
    setUpdateLoading(true);
    const body = {};
    if (updateForm.name) body.name = updateForm.name;
    if (updateForm.gender) body.gender = updateForm.gender;
    if (updateForm.image) body.image = updateForm.image;
    setUpdateRes(await apiCall('PUT', `/users/${updateId}`, body, token));
    setUpdateLoading(false);
  };

  // ── Delete User ──
  const [deleteId, setDeleteId] = useState('');
  const [deleteRes, setDeleteRes] = useState(null);
  const [deleteLoading, setDeleteLoading] = useState(false);
  const doDelete = async () => {
    setDeleteLoading(true);
    setDeleteRes(await apiCall('DELETE', `/users/${deleteId}`, null, token));
    setDeleteLoading(false);
  };

  // ── Change Password ──
  const [cpId, setCpId] = useState('');
  const [cpForm, setCpForm] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' });
  const [cpRes, setCpRes] = useState(null);
  const [cpLoading, setCpLoading] = useState(false);
  const doChangePassword = async () => {
    setCpLoading(true);
    setCpRes(await apiCall('POST', `/users/${cpId}/change-password`, cpForm, token));
    setCpLoading(false);
  };

  // ── Create User (Admin) ──
  const [createForm, setCreateForm] = useState({ name: '', email: '', username: '', password: '', phoneNumber: '', gender: 'MALE' });
  const [createRes, setCreateRes] = useState(null);
  const [createLoading, setCreateLoading] = useState(false);
  const doCreate = async () => {
    setCreateLoading(true);
    setCreateRes(await apiCall('POST', '/users/create-user', createForm, token));
    setCreateLoading(false);
  };

  return (
    <div className="endpoints-grid">
      {/* GET USER BY ID */}
      <EndpointCard method="GET" path="/api/v1/users/{id}" description="Get user by ID (requires admin:read or own user:read)">
        <input className="form-input" placeholder="User ID" value={getUserId} onChange={e => setGetUserId(e.target.value)} />
        <button className="btn" onClick={doGetUser} disabled={getUserLoading}>{getUserLoading ? <div className="spinner"/> : 'Send Request'}</button>
        <ResponsePanel response={getUserRes} loading={getUserLoading} />
      </EndpointCard>

      {/* GET USER BY EMAIL */}
      <EndpointCard method="GET" path="/api/v1/users/getByEmail" description="Get user by email (requires admin:read or user:read)">
        <input className="form-input" placeholder="Email" value={getEmailForm.email} onChange={e => setGetEmailForm({ email: e.target.value })} />
        <button className="btn" onClick={doGetEmail} disabled={getEmailLoading}>{getEmailLoading ? <div className="spinner"/> : 'Send Request'}</button>
        <ResponsePanel response={getEmailRes} loading={getEmailLoading} />
      </EndpointCard>

      {/* UPDATE USER */}
      <EndpointCard method="PUT" path="/api/v1/users/{id}" description="Update user profile (name, gender, image)">
        <input className="form-input" placeholder="User ID" value={updateId} onChange={e => setUpdateId(e.target.value)} />
        <div className="form-row">
          <input className="form-input" placeholder="Name" value={updateForm.name} onChange={e => setUpdateForm({...updateForm, name: e.target.value})} />
          <select className="form-input" value={updateForm.gender} onChange={e => setUpdateForm({...updateForm, gender: e.target.value})}>
            <option value="MALE">MALE</option><option value="FEMALE">FEMALE</option><option value="OTHER">OTHER</option>
          </select>
        </div>
        <button className="btn" onClick={doUpdate} disabled={updateLoading}>{updateLoading ? <div className="spinner"/> : 'Send Request'}</button>
        <ResponsePanel response={updateRes} loading={updateLoading} />
      </EndpointCard>

      {/* DELETE USER */}
      <EndpointCard method="DELETE" path="/api/v1/users/{id}" description="Delete user (admin:delete or own user:delete)">
        <input className="form-input" placeholder="User ID" value={deleteId} onChange={e => setDeleteId(e.target.value)} />
        <button className="btn btn-danger" onClick={doDelete} disabled={deleteLoading}>{deleteLoading ? <div className="spinner"/> : 'Send Request'}</button>
        <ResponsePanel response={deleteRes} loading={deleteLoading} />
      </EndpointCard>

      {/* CHANGE PASSWORD */}
      <EndpointCard method="POST" path="/api/v1/users/{id}/change-password" description="Change own password (requires current password)">
        <input className="form-input" placeholder="User ID" value={cpId} onChange={e => setCpId(e.target.value)} />
        <input className="form-input" type="password" placeholder="Current Password" value={cpForm.currentPassword} onChange={e => setCpForm({...cpForm, currentPassword: e.target.value})} />
        <div className="form-row">
          <input className="form-input" type="password" placeholder="New Password (min 8)" value={cpForm.newPassword} onChange={e => setCpForm({...cpForm, newPassword: e.target.value})} />
          <input className="form-input" type="password" placeholder="Confirm Password" value={cpForm.confirmPassword} onChange={e => setCpForm({...cpForm, confirmPassword: e.target.value})} />
        </div>
        <button className="btn" onClick={doChangePassword} disabled={cpLoading}>{cpLoading ? <div className="spinner"/> : 'Send Request'}</button>
        <ResponsePanel response={cpRes} loading={cpLoading} />
      </EndpointCard>

      {/* CREATE USER (Admin) */}
      <EndpointCard method="POST" path="/api/v1/users/create-user" description="Admin: Create user with specific role (requires admin:create)">
        <div className="form-row">
          <input className="form-input" placeholder="Name" value={createForm.name} onChange={e => setCreateForm({...createForm, name: e.target.value})} />
          <input className="form-input" placeholder="Username" value={createForm.username} onChange={e => setCreateForm({...createForm, username: e.target.value})} />
        </div>
        <div className="form-row">
          <input className="form-input" placeholder="Email" value={createForm.email} onChange={e => setCreateForm({...createForm, email: e.target.value})} />
          <input className="form-input" placeholder="Phone (10 digits)" value={createForm.phoneNumber} onChange={e => setCreateForm({...createForm, phoneNumber: e.target.value})} />
        </div>
        <div className="form-row">
          <input className="form-input" type="password" placeholder="Password" value={createForm.password} onChange={e => setCreateForm({...createForm, password: e.target.value})} />
          <select className="form-input" value={createForm.gender} onChange={e => setCreateForm({...createForm, gender: e.target.value})}>
            <option value="MALE">MALE</option><option value="FEMALE">FEMALE</option><option value="OTHER">OTHER</option>
          </select>
        </div>
        <button className="btn" onClick={doCreate} disabled={createLoading}>{createLoading ? <div className="spinner"/> : 'Send Request'}</button>
        <ResponsePanel response={createRes} loading={createLoading} />
      </EndpointCard>
    </div>
  );
}

// ══════════════════════════════════════════════════════════════════
//  ADMIN TAB
// ══════════════════════════════════════════════════════════════════

function AdminTab({ token }) {
  // ── List Roles ──
  const [rolesRes, setRolesRes] = useState(null);
  const [rolesLoading, setRolesLoading] = useState(false);
  const doGetRoles = async () => { setRolesLoading(true); setRolesRes(await apiCall('GET', '/admin/roles', null, token)); setRolesLoading(false); };

  // ── Create Role ──
  const [createRoleForm, setCreateRoleForm] = useState({ roleName: '', description: '' });
  const [createRoleRes, setCreateRoleRes] = useState(null);
  const [createRoleLoading, setCreateRoleLoading] = useState(false);
  const doCreateRole = async () => { setCreateRoleLoading(true); setCreateRoleRes(await apiCall('POST', '/admin/roles', createRoleForm, token)); setCreateRoleLoading(false); };

  // ── Delete Role ──
  const [deleteRoleId, setDeleteRoleId] = useState('');
  const [deleteRoleRes, setDeleteRoleRes] = useState(null);
  const [deleteRoleLoading, setDeleteRoleLoading] = useState(false);
  const doDeleteRole = async () => { setDeleteRoleLoading(true); setDeleteRoleRes(await apiCall('DELETE', `/admin/roles/${deleteRoleId}`, null, token)); setDeleteRoleLoading(false); };

  // ── Assign Permissions to Role ──
  const [assignPermRoleId, setAssignPermRoleId] = useState('');
  const [assignPermIds, setAssignPermIds] = useState('');
  const [assignPermRes, setAssignPermRes] = useState(null);
  const [assignPermLoading, setAssignPermLoading] = useState(false);
  const doAssignPerms = async () => {
    setAssignPermLoading(true);
    const ids = assignPermIds.split(',').map(s => s.trim()).filter(Boolean);
    setAssignPermRes(await apiCall('POST', `/admin/roles/${assignPermRoleId}/permissions`, { permissionIds: ids }, token));
    setAssignPermLoading(false);
  };

  // ── Revoke Permissions from Role ──
  const [revokePermRoleId, setRevokePermRoleId] = useState('');
  const [revokePermIds, setRevokePermIds] = useState('');
  const [revokePermRes, setRevokePermRes] = useState(null);
  const [revokePermLoading, setRevokePermLoading] = useState(false);
  const doRevokePerms = async () => {
    setRevokePermLoading(true);
    const ids = revokePermIds.split(',').map(s => s.trim()).filter(Boolean);
    setRevokePermRes(await apiCall('DELETE', `/admin/roles/${revokePermRoleId}/permissions`, { permissionIds: ids }, token));
    setRevokePermLoading(false);
  };

  // ── List Permissions ──
  const [permsRes, setPermsRes] = useState(null);
  const [permsLoading, setPermsLoading] = useState(false);
  const doGetPerms = async () => { setPermsLoading(true); setPermsRes(await apiCall('GET', '/admin/permissions', null, token)); setPermsLoading(false); };

  // ── Create Permission ──
  const [createPermForm, setCreatePermForm] = useState({ name: '', description: '' });
  const [createPermRes, setCreatePermRes] = useState(null);
  const [createPermLoading, setCreatePermLoading] = useState(false);
  const doCreatePerm = async () => { setCreatePermLoading(true); setCreatePermRes(await apiCall('POST', '/admin/permissions', createPermForm, token)); setCreatePermLoading(false); };

  // ── Delete Permission ──
  const [deletePermId, setDeletePermId] = useState('');
  const [deletePermRes, setDeletePermRes] = useState(null);
  const [deletePermLoading, setDeletePermLoading] = useState(false);
  const doDeletePerm = async () => { setDeletePermLoading(true); setDeletePermRes(await apiCall('DELETE', `/admin/permissions/${deletePermId}`, null, token)); setDeletePermLoading(false); };

  // ── List Users ──
  const [usersRes, setUsersRes] = useState(null);
  const [usersLoading, setUsersLoading] = useState(false);
  const doGetUsers = async () => { setUsersLoading(true); setUsersRes(await apiCall('GET', '/admin/users', null, token)); setUsersLoading(false); };

  // ── Assign Roles to User ──
  const [assignRoleUserId, setAssignRoleUserId] = useState('');
  const [assignRoleIds, setAssignRoleIds] = useState('');
  const [assignRoleRes, setAssignRoleRes] = useState(null);
  const [assignRoleLoading, setAssignRoleLoading] = useState(false);
  const doAssignRoles = async () => {
    setAssignRoleLoading(true);
    const ids = assignRoleIds.split(',').map(s => s.trim()).filter(Boolean);
    setAssignRoleRes(await apiCall('PUT', `/admin/users/${assignRoleUserId}/roles`, { roleIds: ids }, token));
    setAssignRoleLoading(false);
  };

  return (
    <div className="endpoints-grid">
      {/* ROLES SECTION */}
      <div className="section-header">Role Management</div>

      <EndpointCard method="GET" path="/api/v1/admin/roles" description="List all roles (requires roles:read)">
        <button className="btn" onClick={doGetRoles} disabled={rolesLoading}>{rolesLoading ? <div className="spinner"/> : 'Send Request'}</button>
        <ResponsePanel response={rolesRes} loading={rolesLoading} />
      </EndpointCard>

      <EndpointCard method="POST" path="/api/v1/admin/roles" description="Create a new role (requires roles:create)">
        <div className="form-row">
          <input className="form-input" placeholder="Role name" value={createRoleForm.roleName} onChange={e => setCreateRoleForm({...createRoleForm, roleName: e.target.value})} />
          <input className="form-input" placeholder="Description" value={createRoleForm.description} onChange={e => setCreateRoleForm({...createRoleForm, description: e.target.value})} />
        </div>
        <button className="btn" onClick={doCreateRole} disabled={createRoleLoading}>{createRoleLoading ? <div className="spinner"/> : 'Send Request'}</button>
        <ResponsePanel response={createRoleRes} loading={createRoleLoading} />
      </EndpointCard>

      <EndpointCard method="DELETE" path="/api/v1/admin/roles/{roleId}" description="Delete a role (requires roles:delete)">
        <input className="form-input" placeholder="Role UUID" value={deleteRoleId} onChange={e => setDeleteRoleId(e.target.value)} />
        <button className="btn btn-danger" onClick={doDeleteRole} disabled={deleteRoleLoading}>{deleteRoleLoading ? <div className="spinner"/> : 'Send Request'}</button>
        <ResponsePanel response={deleteRoleRes} loading={deleteRoleLoading} />
      </EndpointCard>

      <EndpointCard method="POST" path="/api/v1/admin/roles/{roleId}/permissions" description="Assign permissions to a role (requires roles:update)">
        <input className="form-input" placeholder="Role UUID" value={assignPermRoleId} onChange={e => setAssignPermRoleId(e.target.value)} />
        <input className="form-input" placeholder="Permission UUIDs (comma-separated)" value={assignPermIds} onChange={e => setAssignPermIds(e.target.value)} />
        <button className="btn" onClick={doAssignPerms} disabled={assignPermLoading}>{assignPermLoading ? <div className="spinner"/> : 'Send Request'}</button>
        <ResponsePanel response={assignPermRes} loading={assignPermLoading} />
      </EndpointCard>

      <EndpointCard method="DELETE" path="/api/v1/admin/roles/{roleId}/permissions" description="Revoke permissions from a role (requires roles:update)">
        <input className="form-input" placeholder="Role UUID" value={revokePermRoleId} onChange={e => setRevokePermRoleId(e.target.value)} />
        <input className="form-input" placeholder="Permission UUIDs (comma-separated)" value={revokePermIds} onChange={e => setRevokePermIds(e.target.value)} />
        <button className="btn btn-danger" onClick={doRevokePerms} disabled={revokePermLoading}>{revokePermLoading ? <div className="spinner"/> : 'Send Request'}</button>
        <ResponsePanel response={revokePermRes} loading={revokePermLoading} />
      </EndpointCard>

      {/* PERMISSIONS SECTION */}
      <div className="section-header">Permission Management</div>

      <EndpointCard method="GET" path="/api/v1/admin/permissions" description="List all permissions (requires permissions:read)">
        <button className="btn" onClick={doGetPerms} disabled={permsLoading}>{permsLoading ? <div className="spinner"/> : 'Send Request'}</button>
        <ResponsePanel response={permsRes} loading={permsLoading} />
      </EndpointCard>

      <EndpointCard method="POST" path="/api/v1/admin/permissions" description="Create a new permission (requires permissions:create)">
        <div className="form-row">
          <input className="form-input" placeholder="Permission name (e.g. users:read)" value={createPermForm.name} onChange={e => setCreatePermForm({...createPermForm, name: e.target.value})} />
          <input className="form-input" placeholder="Description" value={createPermForm.description} onChange={e => setCreatePermForm({...createPermForm, description: e.target.value})} />
        </div>
        <button className="btn" onClick={doCreatePerm} disabled={createPermLoading}>{createPermLoading ? <div className="spinner"/> : 'Send Request'}</button>
        <ResponsePanel response={createPermRes} loading={createPermLoading} />
      </EndpointCard>

      <EndpointCard method="DELETE" path="/api/v1/admin/permissions/{permissionId}" description="Delete a permission (requires permissions:delete)">
        <input className="form-input" placeholder="Permission UUID" value={deletePermId} onChange={e => setDeletePermId(e.target.value)} />
        <button className="btn btn-danger" onClick={doDeletePerm} disabled={deletePermLoading}>{deletePermLoading ? <div className="spinner"/> : 'Send Request'}</button>
        <ResponsePanel response={deletePermRes} loading={deletePermLoading} />
      </EndpointCard>

      {/* USER MANAGEMENT */}
      <div className="section-header">User Role Management</div>

      <EndpointCard method="GET" path="/api/v1/admin/users" description="List all users with pagination (requires users:read)">
        <button className="btn" onClick={doGetUsers} disabled={usersLoading}>{usersLoading ? <div className="spinner"/> : 'Send Request'}</button>
        <ResponsePanel response={usersRes} loading={usersLoading} />
      </EndpointCard>

      <EndpointCard method="PUT" path="/api/v1/admin/users/{userId}/roles" description="Assign roles to a user (requires users:assign-roles)">
        <input className="form-input" placeholder="User ID (Long)" value={assignRoleUserId} onChange={e => setAssignRoleUserId(e.target.value)} />
        <input className="form-input" placeholder="Role UUIDs (comma-separated)" value={assignRoleIds} onChange={e => setAssignRoleIds(e.target.value)} />
        <button className="btn" onClick={doAssignRoles} disabled={assignRoleLoading}>{assignRoleLoading ? <div className="spinner"/> : 'Send Request'}</button>
        <ResponsePanel response={assignRoleRes} loading={assignRoleLoading} />
      </EndpointCard>
    </div>
  );
}

export default App;
