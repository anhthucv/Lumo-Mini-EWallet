import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';

import { getUnreadNotificationCount } from '../api/notificationApi';
import { ApiError } from '../api/http';
import { useAuth } from '../auth/AuthContext';

interface NotificationContextValue {
  unreadCount: number;
  unreadCountError: string | null;
  refreshUnreadCount: () => Promise<void>;
}

const NotificationContext = createContext<NotificationContextValue | undefined>(undefined);

export function NotificationProvider({ children }: { children: ReactNode }) {
  const navigate = useNavigate();
  const { isAuthenticated, logout } = useAuth();
  const [unreadCount, setUnreadCount] = useState(0);
  const [unreadCountError, setUnreadCountError] = useState<string | null>(null);

  async function refreshUnreadCount(signal?: AbortSignal) {
    try {
      const response = await getUnreadNotificationCount(signal);
      setUnreadCount(Math.max(0, response.unreadCount));
      setUnreadCountError(null);
    } catch (error) {
      if (signal?.aborted) return;
      if (error instanceof ApiError && error.status === 401) {
        logout();
        navigate('/login', { replace: true });
        return;
      }
      setUnreadCountError('Could not load notification count.');
    }
  }

  useEffect(() => {
    if (!isAuthenticated) {
      setUnreadCount(0);
      setUnreadCountError(null);
      return;
    }
    const controller = new AbortController();
    void refreshUnreadCount(controller.signal);
    return () => controller.abort();
  }, [isAuthenticated]);

  return (
    <NotificationContext.Provider value={{ unreadCount, unreadCountError, refreshUnreadCount }}>
      {children}
    </NotificationContext.Provider>
  );
}

export function useNotifications(): NotificationContextValue {
  const context = useContext(NotificationContext);
  if (!context) {
    throw new Error('useNotifications must be used within a NotificationProvider.');
  }
  return context;
}
