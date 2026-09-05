import { requestJson } from './http';
import type { TopUpResponse } from '../types/wallet';

export function createTopUp(amount: number, idempotencyKey: string, signal?: AbortSignal): Promise<TopUpResponse> {
  return requestJson<TopUpResponse>('/topups', {
    method: 'POST',
    headers: { 'Idempotency-Key': idempotencyKey },
    body: JSON.stringify({ amount }),
    signal,
  });
}

export function getTopUp(id: number, signal?: AbortSignal): Promise<TopUpResponse> {
  return requestJson<TopUpResponse>(`/topups/${id}`, {
    method: 'GET',
    signal,
  });
}

export function syncTopUp(id: number, signal?: AbortSignal): Promise<TopUpResponse> {
  return requestJson<TopUpResponse>(`/topups/${id}/sync`, {
    method: 'POST',
    signal,
  });
}
