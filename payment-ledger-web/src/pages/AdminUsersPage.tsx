import { useEffect, useState, type FormEvent } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';

import { getAdminUsers, lockAdminUser, unlockAdminUser } from '../api/adminUserApi';
import { ApiError } from '../api/http';
import { useAuth } from '../auth/AuthContext';
import NotificationBell from '../components/NotificationBell';
import type { AdminUser } from '../types/admin';
import './admin-users.css';
import './dashboard.css';

const money = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 });

function formatDate(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : new Intl.DateTimeFormat('en', { dateStyle: 'medium' }).format(date);
}

function formatBalance(value: AdminUser['balance']) {
  const amount = value === null ? NaN : Number(value);
  return Number.isFinite(amount) ? money.format(amount) : 'No account';
}

function errorMessage(error: unknown) {
  if (error instanceof ApiError && error.status === 403) return 'You do not have permission to manage users.';
  if (error instanceof ApiError && error.status === 404) return 'That user no longer exists.';
  return 'Users could not be loaded. Please try again.';
}

export default function AdminUsersPage() {
  const { user, isAuthenticated, isHydrating, logout } = useAuth();
  const navigate = useNavigate();
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [search, setSearch] = useState('');
  const [draftSearch, setDraftSearch] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);
  const [target, setTarget] = useState<AdminUser | null>(null);
  const [reason, setReason] = useState('');
  const [actionError, setActionError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [success, setSuccess] = useState<string | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    async function load() {
      setLoading(true); setError(null);
      try {
        const response = await getAdminUsers(page, 10, search, controller.signal);
        setUsers(response.content); setTotalPages(response.totalPages);
      } catch (requestError) {
        if (controller.signal.aborted) return;
        if (requestError instanceof ApiError && requestError.status === 401) { logout(); navigate('/login', { replace: true }); return; }
        setError(errorMessage(requestError));
      } finally { if (!controller.signal.aborted) setLoading(false); }
    }
    void load();
    return () => controller.abort();
  }, [logout, navigate, page, refreshKey, search]);

  if (isHydrating) return <div className="auth-loading" role="status">Restoring your session...</div>;
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  if (user?.role !== 'ADMIN') return <Navigate to="/dashboard" replace />;

  function applySearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setSearch(draftSearch); setPage(0);
  }

  function openStatusChange(account: AdminUser) {
    setTarget(account); setReason(''); setActionError(null); setSuccess(null);
  }

  function closeModal() { if (!saving) setTarget(null); }

  async function submitStatusChange(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!target || !reason.trim()) { setActionError('A reason is required.'); return; }
    setSaving(true); setActionError(null);
    try {
      if (target.status === 'ACTIVE') await lockAdminUser(target.userId, reason.trim());
      else await unlockAdminUser(target.userId, reason.trim());
      setTarget(null); setSuccess('User status updated.'); setRefreshKey((key) => key + 1);
    } catch (requestError) {
      if (requestError instanceof ApiError && requestError.status === 401) { logout(); navigate('/login', { replace: true }); return; }
      setActionError(errorMessage(requestError));
    } finally { setSaving(false); }
  }

  return (
    <main className="dashboard-shell admin-users-shell">
      <div className="dashboard-container">
        <header className="dashboard-header">
          <Link to="/dashboard" className="dashboard-brand" aria-label="Lumo home"><span className="dashboard-brand-mark" aria-hidden="true"><i /></span><strong>Lumo</strong></Link>
          <nav className="dashboard-nav" aria-label="Main navigation"><Link to="/dashboard">Home</Link><Link to="/wallet">Wallet</Link><Link to="/activity">Activity</Link><Link className="active" to="/admin/users">Admin users</Link></nav>
          <div className="dashboard-header-actions"><NotificationBell /><Link className="admin-profile-link" to="/profile">Profile</Link></div>
        </header>
        <section className="admin-users-heading"><span className="dashboard-eyebrow">Administration</span><h1>User accounts</h1><p>Review account access and wallet status without exposing sensitive credentials.</p></section>
        <section className="admin-users-surface">
          <form className="admin-users-search" onSubmit={applySearch}><label htmlFor="admin-user-search">Search users</label><div><input id="admin-user-search" value={draftSearch} onChange={(event) => setDraftSearch(event.target.value)} placeholder="Email or full name" /><button className="primary-button" type="submit">Search</button></div></form>
          {success && <p className="admin-users-success" role="status">{success}</p>}
          {loading && <div className="admin-users-state" role="status">Loading users...</div>}
          {!loading && error && <div className="admin-users-state admin-users-error" role="alert">{error}<button className="secondary-button" type="button" onClick={() => setRefreshKey((key) => key + 1)}>Try again</button></div>}
          {!loading && !error && users.length === 0 && <div className="admin-users-state">No users match this search.</div>}
          {!loading && !error && users.length > 0 && <div className="admin-users-table-wrap"><table><thead><tr><th>User</th><th>Role</th><th>Status</th><th>Wallet</th><th>Created</th><th><span className="sr-only">Actions</span></th></tr></thead><tbody>{users.map((account) => <tr key={account.userId}><td><strong>{account.fullName}</strong><small>{account.email}</small></td><td>{account.role}</td><td><span className={`admin-status ${account.status.toLowerCase()}`}>{account.status}</span></td><td><strong>{account.accountNumberSummary ?? 'No account'}</strong><small>{formatBalance(account.balance)}</small></td><td>{formatDate(account.createdAt)}</td><td><button className="admin-status-button" type="button" onClick={() => openStatusChange(account)}>{account.status === 'ACTIVE' ? 'Lock' : 'Unlock'}</button></td></tr>)}</tbody></table></div>}
          {!loading && !error && totalPages > 1 && <div className="admin-users-pagination"><button className="secondary-button" type="button" disabled={page === 0} onClick={() => setPage((value) => value - 1)}>Previous</button><span>Page {page + 1} of {totalPages}</span><button className="secondary-button" type="button" disabled={page + 1 >= totalPages} onClick={() => setPage((value) => value + 1)}>Next</button></div>}
        </section>
      </div>
      {target && <div className="admin-modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) closeModal(); }}><section className="admin-modal" role="dialog" aria-modal="true" aria-labelledby="admin-status-title"><h2 id="admin-status-title">{target.status === 'ACTIVE' ? 'Lock user' : 'Unlock user'}</h2><p>{target.email}</p><form onSubmit={submitStatusChange}><label htmlFor="status-reason">Reason</label><textarea id="status-reason" value={reason} onChange={(event) => setReason(event.target.value)} maxLength={255} required autoFocus placeholder="Explain this status change" />{actionError && <div className="admin-users-error" role="alert">{actionError}</div>}<div className="admin-modal-actions"><button className="secondary-button" type="button" onClick={closeModal} disabled={saving}>Cancel</button><button className="primary-button" type="submit" disabled={saving}>{saving ? 'Saving...' : 'Confirm'}</button></div></form></section></div>}
    </main>
  );
}
