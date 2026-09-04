import type {
  MarkAllNotificationsReadResponse,
  Notification,
  NotificationPage,
  UnreadNotificationCount,
} from '../types/notification';
import { requestJson } from './http';

export function getNotifications(page = 0, size = 20, signal?: AbortSignal): Promise<NotificationPage> {
  const query = new URLSearchParams({ page: String(page), size: String(size) });
  return requestJson<NotificationPage>(`/notifications?${query.toString()}`, {
    method: 'GET',
    signal,
  });
}

export function getUnreadNotificationCount(signal?: AbortSignal): Promise<UnreadNotificationCount> {
  return requestJson<UnreadNotificationCount>('/notifications/unread-count', {
    method: 'GET',
    signal,
  });
}

export function markNotificationRead(id: number, signal?: AbortSignal): Promise<Notification> {
  return requestJson<Notification>(`/notifications/${id}/read`, {
    method: 'PATCH',
    signal,
  });
}

export function markAllNotificationsRead(signal?: AbortSignal): Promise<MarkAllNotificationsReadResponse> {
  return requestJson<MarkAllNotificationsReadResponse>('/notifications/read-all', {
    method: 'PATCH',
    signal,
  });
}
