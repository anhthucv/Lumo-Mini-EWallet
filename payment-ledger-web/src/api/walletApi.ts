import type {
  DepositRequest,
  DepositResponse,
  MyWalletResponse,
  RecipientResponse,
  TransferRequest,
  TransferResponse,
} from '../types/wallet';
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

export function withdraw(amount: number, signal?: AbortSignal): Promise<DepositResponse> {
  const request: DepositRequest = { amount };
  return requestJson<DepositResponse>('/wallet/withdraw', {
    method: 'POST',
    body: JSON.stringify(request),
    signal,
  });
}

export function getRecipient(accountNumber: string, signal?: AbortSignal): Promise<RecipientResponse> {
  const query = new URLSearchParams({ accountNumber });
  return requestJson<RecipientResponse>(`/wallet/recipient?${query.toString()}`, {
    method: 'GET',
    signal,
  });
}

export function transfer(
  recipientAccountNumber: string,
  amount: number,
  signal?: AbortSignal,
): Promise<TransferResponse> {
  const request: TransferRequest = { recipientAccountNumber, amount };
  return requestJson<TransferResponse>('/wallet/transfer', {
    method: 'POST',
    body: JSON.stringify(request),
    signal,
  });
}
