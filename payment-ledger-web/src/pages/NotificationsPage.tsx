import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';

import {
  getNotifications,
  markAllNotificationsRead,
  markNotificationRead,
} from '../api/notificationApi';
import { ApiError } from '../api/http';
import { useAuth } from '../auth/AuthContext';
import { useNotifications } from '../notifications/NotificationContext';
import type { Notification } from '../types/notification';
import NotificationBell from '../components/NotificationBell';
import './notifications-page.css';
import '../components/notifications.css';
import './dashboard.css';

const PAGE_SIZE = 20;
const amountFormatter = new Intl.NumberFormat('vi-VN', {
  style: 'currency',
  currency: 'VND',
  maximumFractionDigits: 0,
});

function formatAmount(amount: Notification['amount']): string {
  return amount === null || !Number.isFinite(Number(amount))
    ? 'Amount unavailable'
    : amountFormatter.format(Number(amount));
}

function formatDate(createdAt: string): string {
  const date = new Date(createdAt);
  return Number.isNaN(date.getTime())
    ? createdAt
    : new Intl.DateTimeFormat('en', { dateStyle: 'medium', timeStyle: 'short' }).format(date);
}

function iconFor(type: Notification['type']): string {
  if (type === 'DEPOSIT_SUCCESS') return '+';
  if (type === 'TRANSFER_RECEIVED') return '↙';
  return '↗';
}

export default function NotificationsPage() {
  const navigate = useNavigate();
  const { logout } = useAuth();
  const { unreadCount, refreshUnreadCount } = useNotifications();
  const [items, setItems] = useState<Notification[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [markingIds, setMarkingIds] = useState<Set<number>>(new Set());
  const [markingAll, setMarkingAll] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const [retryKey, setRetryKey] = useState(0);

  function handleUnauthorized(requestError: unknown): boolean {
    if (requestError instanceof ApiError && requestError.status === 401) {
      logout();
      navigate('/login', { replace: true });
      return true;
    }
    return false;
  }

  useEffect(() => {
    const controller = new AbortController();
    async function loadPage() {
      setLoading(true);
      setError(null);
      try {
        const response = await getNotifications(page, PAGE_SIZE, controller.signal);
        setItems(response.content);
        setTotalPages(response.totalPages);
        void refreshUnreadCount();
      } catch (requestError) {
        if (controller.signal.aborted) return;
        if (!handleUnauthorized(requestError)) setError('Could not load notifications.');
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    }
    void loadPage();
    return () => controller.abort();
  }, [page, retryKey]);

  async function handleMarkRead(notification: Notification) {
    if (notification.read || markingIds.has(notification.id) || markingAll) return;
    setMarkingIds((current) => new Set(current).add(notification.id));
    setActionError(null);
    try {
      const updated = await markNotificationRead(notification.id);
      setItems((current) => current.map((item) => item.id === updated.id ? updated : item));
      await refreshUnreadCount();
    } catch (requestError) {
      if (!handleUnauthorized(requestError)) setActionError('Could not mark notification as read.');
    } finally {
      setMarkingIds((current) => {
        const next = new Set(current);
        next.delete(notification.id);
        return next;
      });
    }
  }

  async function handleMarkAllRead() {
    if (markingAll || unreadCount === 0) return;
    setMarkingAll(true);
    setActionError(null);
    try {
      await markAllNotificationsRead();
      const readAt = new Date().toISOString();
      setItems((current) => current.map((item) => ({ ...item, read: true, readAt })));
      await refreshUnreadCount();
    } catch (requestError) {
      if (!handleUnauthorized(requestError)) setActionError('Could not mark all notifications as read.');
    } finally {
      setMarkingAll(false);
    }
  }

  return (
    <main className="dashboard-shell notifications-shell">
      <div className="dashboard-container">
        <header className="dashboard-header">
          <Link to="/dashboard" className="dashboard-brand" aria-label="Lumo home"><span className="dashboard-brand-mark" aria-hidden="true"><i /></span><strong>Lumo</strong></Link>
          <nav className="dashboard-nav" aria-label="Main navigation"><Link to="/dashboard">Home</Link><Link to="/wallet">Wallet</Link><Link to="/activity">Activity</Link></nav>
          <div className="dashboard-header-actions"><NotificationBell /><Link to="/profile" className="dashboard-wallet-profile">Profile</Link></div>
        </header>

        <div className="notifications-heading">
          <div>
            <span className="notification-page-kicker">Incoming money</span>
            <h1 id="notifications-title">Notifications</h1>
            <p>Updates when money arrives in your wallet.</p>
          </div>
          <button
            type="button"
            className="secondary-button"
            onClick={() => void handleMarkAllRead()}
            disabled={markingAll || unreadCount === 0}
          >
            {markingAll ? 'Updating...' : 'Mark all read'}
          </button>
        </div>

        {actionError && <div className="banner error" role="alert">{actionError}</div>}
        {loading && <div className="notification-page-state" role="status">Loading notifications...</div>}
        {!loading && error && (
          <div className="notification-page-state notification-page-error" role="alert">
            <span>{error}</span>
            <button type="button" className="secondary-button" onClick={() => setRetryKey((current) => current + 1)}>Try again</button>
          </div>
        )}
        {!loading && !error && items.length === 0 && (
          <div className="notification-page-state">
            <strong>You’re all caught up.</strong>
            <span>Money received in your wallet will appear here.</span>
          </div>
        )}
        {!loading && !error && items.length > 0 && (
          <div className="notification-page-list">
            {items.map((notification) => (
              <button
                type="button"
                key={notification.id}
                className={`notification-page-item ${notification.read ? '' : 'unread'}`}
                onClick={() => void handleMarkRead(notification)}
                disabled={markingIds.has(notification.id) || markingAll}
              >
                <span className="notification-page-icon" aria-hidden="true">{iconFor(notification.type)}</span>
                <span className="notification-page-copy">
                  <strong>{notification.title}</strong>
                  <span>{notification.message}</span>
                  <small>{formatDate(notification.createdAt)} · {formatAmount(notification.amount)}</small>
                </span>
                {!notification.read && <span className="notification-unread-dot" aria-label="Unread" />}
              </button>
            ))}
          </div>
        )}

        {!loading && !error && totalPages > 1 && (
          <div className="notification-pagination" aria-label="Notification pagination">
            <button type="button" className="secondary-button" onClick={() => setPage((current) => current - 1)} disabled={page === 0}>Previous</button>
            <span>Page {page + 1} of {totalPages}</span>
            <button type="button" className="secondary-button" onClick={() => setPage((current) => current + 1)} disabled={page >= totalPages - 1}>Next</button>
          </div>
        )}
      </div>
    </main>
  );
}
