import { requestJson } from './http';
import type { AdminDashboard } from '../types/adminTransactions';
import type { AdminTransaction, AdminTransactionDetail } from '../types/adminTransactions';
import type { AdminAuditLog } from '../types/adminAudit';

function query(params: Record<string, string | undefined>) { const value = new URLSearchParams(); Object.entries(params).forEach(([key, item]) => { if (item) value.set(key, item); }); return value.toString(); }
export function getAdminDashboard() { return requestJson<AdminDashboard>('/api/admin/dashboard', { method: 'GET' }); }
export function getAdminTransactions(params: Record<string, string | undefined>, signal?: AbortSignal) { return requestJson<{ content: AdminTransaction[]; totalPages: number; number: number; first: boolean; last: boolean }>(`/api/admin/transactions?${query(params)}`, { method: 'GET', signal }); }
export function getAdminTransaction(id: number) { return requestJson<AdminTransactionDetail>(`/api/admin/transactions/${id}`, { method: 'GET' }); }
export function getAdminAuditLogs(params: Record<string, string | undefined>, signal?: AbortSignal) { return requestJson<{ content: AdminAuditLog[]; totalPages: number; number: number; first: boolean; last: boolean }>(`/api/admin/audit-logs?${query(params)}`, { method: 'GET', signal }); }
export function getAdminAuditLog(id: number) { return requestJson<AdminAuditLog>(`/api/admin/audit-logs/${id}`, { method: 'GET' }); }
