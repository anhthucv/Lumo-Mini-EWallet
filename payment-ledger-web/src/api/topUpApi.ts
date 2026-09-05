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
