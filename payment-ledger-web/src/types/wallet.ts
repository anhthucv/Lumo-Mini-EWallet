export interface MyWalletResponse {
  accountId: number;
  accountNumber: string;
  ownerName: string;
  balance: number | string;
  status: string;
}

export interface DepositRequest {
  amount: number;
}

export interface DepositResponse {
  id: number;
  accountNumber: string;
  ownerName: string;
  balance: number | string;
  status: string;
}

export interface RecipientResponse {
  accountNumber: string;
  ownerName: string;
}

export interface TransactionLimit {
  perTransactionLimit: number | string;
  dailyLimit: number | string;
  usedToday: number | string;
  remainingToday: number | string;
}

export interface WalletLimitsResponse {
  deposit: TransactionLimit;
  withdraw: TransactionLimit;
  transfer: TransactionLimit;
}

export interface TransferRequest {
  recipientAccountNumber: string;
  amount: number;
}

export type TransferResponse = DepositResponse;

export type TransactionType = 'DEPOSIT' | 'WITHDRAW' | 'TRANSFER_IN' | 'TRANSFER_OUT';

export interface TransactionResponse {
  id: number;
  accountId: number;
  relatedAccountId: number | null;
  transactionType: TransactionType;
  amount: number | string;
  balanceAfterTransaction: number | string;
  createdAt: string;
}

export interface TransactionPageResponse {
  content: TransactionResponse[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface TransactionFilters {
  type?: TransactionType;
  fromDate?: string;
  toDate?: string;
  minAmount?: string;
  maxAmount?: string;
}
