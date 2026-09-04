import type {
  DepositRequest,
  DepositResponse,
  MyWalletResponse,
  RecipientResponse,
  TransactionPageResponse,
  TransactionResponse,
  TransactionFilters,
  TransferRequest,
  TransferResponse,
  WalletLimitsResponse,
} from '../types/wallet';
import { requestJson } from './http';

export function getMyWallet(signal?: AbortSignal): Promise<MyWalletResponse> {
  return requestJson<MyWalletResponse>('/wallet/me', {
    method: 'GET',
    signal,
  });
}

export function getWalletLimits(signal?: AbortSignal): Promise<WalletLimitsResponse> {
  return requestJson<WalletLimitsResponse>('/wallet/limits', {
    method: 'GET',
    signal,
  });
}

export function deposit(amount: number, idempotencyKey: string, signal?: AbortSignal): Promise<DepositResponse> {
  const request: DepositRequest = { amount };
  return requestJson<DepositResponse>('/wallet/deposit', {
    method: 'POST',
    body: JSON.stringify(request),
    headers: { 'Idempotency-Key': idempotencyKey },
    signal,
  });
}

export function withdraw(amount: number, idempotencyKey: string, signal?: AbortSignal): Promise<DepositResponse> {
  const request: DepositRequest = { amount };
  return requestJson<DepositResponse>('/wallet/withdraw', {
    method: 'POST',
    body: JSON.stringify(request),
    headers: { 'Idempotency-Key': idempotencyKey },
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
  idempotencyKey: string,
  signal?: AbortSignal,
): Promise<TransferResponse> {
  const request: TransferRequest = { recipientAccountNumber, amount };
  return requestJson<TransferResponse>('/wallet/transfer', {
    method: 'POST',
    body: JSON.stringify(request),
    headers: { 'Idempotency-Key': idempotencyKey },
    signal,
  });
}

export function getTransactions({
  page = 0,
  size = 10,
  sort = 'createdAt,desc',
  signal,
  ...filters
}: TransactionFilters & {
  page?: number;
  size?: number;
  sort?: string;
  signal?: AbortSignal;
} = {}): Promise<TransactionPageResponse> {
  const query = new URLSearchParams({ page: String(page), size: String(size), sort });
  Object.entries(filters).forEach(([key, value]) => {
    if (value) {
      query.set(key, value);
    }
  });
  return requestJson<TransactionPageResponse>(`/transactions?${query.toString()}`, {
    method: 'GET',
    signal,
  });
}

export function getTransaction(id: number, signal?: AbortSignal): Promise<TransactionResponse> {
  return requestJson<TransactionResponse>(`/transactions/${id}`, {
    method: 'GET',
    signal,
  });
}
