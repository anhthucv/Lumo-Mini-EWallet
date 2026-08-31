import type { DepositRequest, DepositResponse, MyWalletResponse } from '../types/wallet';
import { requestJson } from './http';

export function getMyWallet(signal?: AbortSignal): Promise<MyWalletResponse> {
  return requestJson<MyWalletResponse>('/wallet/me', {
    method: 'GET',
    signal,
  });
}

export function deposit(amount: number, signal?: AbortSignal): Promise<DepositResponse> {
  const request: DepositRequest = { amount };
  return requestJson<DepositResponse>('/wallet/deposit', {
    method: 'POST',
    body: JSON.stringify(request),
    signal,
  });
}
