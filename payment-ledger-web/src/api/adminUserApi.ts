import type { AdminUsersPage } from '../types/admin';
import { requestJson } from './http';

function usersUrl(page: number, size: number, search: string) {
  const query = new URLSearchParams({ page: String(page), size: String(size), sort: 'createdAt,desc' });
  if (search.trim()) query.set('search', search.trim());
  return `/api/admin/users?${query.toString()}`;
}

export function getAdminUsers(page = 0, size = 10, search = '', signal?: AbortSignal) {
  return requestJson<AdminUsersPage>(usersUrl(page, size, search), { method: 'GET', signal });
}

function changeStatus(userId: number, action: 'lock' | 'unlock', reason: string) {
  return requestJson(`/api/admin/users/${userId}/${action}`, {
    method: 'POST',
    body: JSON.stringify({ reason }),
  });
}

export function lockAdminUser(userId: number, reason: string) {
  return changeStatus(userId, 'lock', reason);
}

export function unlockAdminUser(userId: number, reason: string) {
  return changeStatus(userId, 'unlock', reason);
}
