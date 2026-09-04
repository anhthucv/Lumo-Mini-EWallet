import { useEffect, useRef, useState } from 'react';
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
import './notifications.css';

const amountFormatter = new Intl.NumberFormat('vi-VN', {
  style: 'currency',
  currency: 'VND',
  maximumFractionDigits: 0,
});

function formatAmount(amount: Notification['amount']): string {
  if (amount === null || !Number.isFinite(Number(amount))) return 'an amount';
  return amountFormatter.format(Number(amount));
}

function formatRelativeTime(createdAt: string): string {
  const timestamp = new Date(createdAt).getTime();
  if (!Number.isFinite(timestamp)) return createdAt;
  const seconds = Math.max(0, Math.floor((Date.now() - timestamp) / 1000));
  if (seconds < 60) return 'Just now';
  if (seconds < 3600) return `${Math.floor(seconds / 60)}m ago`;
  if (seconds < 86400) return `${Math.floor(seconds / 3600)}h ago`;
  if (seconds < 172800) return 'Yesterday';
  return new Intl.DateTimeFormat('en', { dateStyle: 'medium' }).format(new Date(timestamp));
}

function getNotificationIcon(type: Notification['type']): string {
  if (type === 'DEPOSIT_SUCCESS') return '+';
  if (type === 'TRANSFER_RECEIVED') return '↙';
  return '↗';
}

function BellIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <path d="M18 9a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4" />
    </svg>
  );
}

export default function NotificationBell() {
  const navigate = useNavigate();
  const { logout } = useAuth();
  const { unreadCount, unreadCountError, refreshUnreadCount } = useNotifications();
  const [open, setOpen] = useState(false);
  const [items, setItems] = useState<Notification[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [markingIds, setMarkingIds] = useState<Set<number>>(new Set());
  const [markingAll, setMarkingAll] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  function handleUnauthorized(requestError: unknown): boolean {
    if (requestError instanceof ApiError && requestError.status === 401) {
      logout();
      navigate('/login', { replace: true });
      return true;
    }
    return false;
  }

  async function loadPreview() {
    setLoading(true);
    setError(null);
    try {
      const response = await getNotifications(0, 5);
      setItems(response.content);
      await refreshUnreadCount();
    } catch (requestError) {
      if (!handleUnauthorized(requestError)) setError('Could not load notifications.');
    } finally {
      setLoading(false);
    }
  }

  function toggleOpen() {
    const nextOpen = !open;
    setOpen(nextOpen);
    if (nextOpen) void loadPreview();
  }

  useEffect(() => {
    if (!open) return undefined;

    function handlePointerDown(event: PointerEvent) {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    }
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') setOpen(false);
    }
    document.addEventListener('pointerdown', handlePointerDown);
    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('pointerdown', handlePointerDown);
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [open]);

  async function handleMarkRead(notification: Notification) {
    if (notification.read || markingIds.has(notification.id) || markingAll) return;
    setMarkingIds((current) => new Set(current).add(notification.id));
    try {
      const updated = await markNotificationRead(notification.id);
      setItems((current) => current.map((item) => item.id === updated.id ? updated : item));
      await refreshUnreadCount();
    } catch (requestError) {
      if (!handleUnauthorized(requestError)) setError('Could not mark notification as read.');
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
    try {
      await markAllNotificationsRead();
      const readAt = new Date().toISOString();
      setItems((current) => current.map((item) => ({ ...item, read: true, readAt })));
      await refreshUnreadCount();
    } catch (requestError) {
      if (!handleUnauthorized(requestError)) setError('Could not mark all notifications as read.');
    } finally {
      setMarkingAll(false);
    }
  }

  const displayCount = unreadCount > 99 ? '99+' : String(unreadCount);

  return (
    <div className="notification-anchor" ref={containerRef}>
      <button
        type="button"
        className="notification-bell"
        aria-label={unreadCount > 0 ? `Notifications, ${unreadCount} unread` : 'Notifications'}
        aria-expanded={open}
        aria-controls="notification-preview"
        onClick={toggleOpen}
      >
        <BellIcon />
        {unreadCount > 0 && <span className="notification-badge" aria-hidden="true">{displayCount}</span>}
      </button>
      {open && (
        <section className="notification-preview" id="notification-preview" aria-label="Notification preview">
          <div className="notification-preview-header">
            <div>
              <h2>Notifications</h2>
              <span className="notification-unread-label">{unreadCount} unread</span>
            </div>
            <button
              type="button"
              className="notification-text-button"
              onClick={() => void handleMarkAllRead()}
              disabled={markingAll || unreadCount === 0}
            >
              {markingAll ? 'Updating...' : 'Mark all read'}
            </button>
          </div>
          {unreadCountError && !error && <p className="notification-inline-error" role="status">{unreadCountError}</p>}
          {loading && <div className="notification-state" role="status">Loading notifications...</div>}
          {!loading && error && (
            <div className="notification-state notification-error" role="alert">
              <span>{error}</span>
              <button type="button" className="notification-text-button" onClick={() => void loadPreview()}>Try again</button>
            </div>
          )}
          {!loading && !error && items.length === 0 && (
            <div className="notification-state">You’re all caught up.</div>
          )}
          {!loading && !error && items.length > 0 && (
            <div className="notification-preview-list">
              {items.map((notification) => (
                <button
                  type="button"
                  key={notification.id}
                  className={`notification-item ${notification.read ? '' : 'unread'}`}
                  onClick={() => void handleMarkRead(notification)}
                  disabled={markingIds.has(notification.id) || markingAll}
                >
                  <span className="notification-item-icon" aria-hidden="true">{getNotificationIcon(notification.type)}</span>
                  <span className="notification-item-copy">
                    <strong>{notification.title}</strong>
                    <span>{notification.message}</span>
                    <small>{formatRelativeTime(notification.createdAt)} · {formatAmount(notification.amount)}</small>
                  </span>
                  {!notification.read && <span className="notification-unread-dot" aria-label="Unread" />}
                </button>
              ))}
            </div>
          )}
          <Link to="/notifications" className="notification-view-all" onClick={() => setOpen(false)}>
            View all notifications <span aria-hidden="true">→</span>
          </Link>
        </section>
      )}
    </div>
  );
}
