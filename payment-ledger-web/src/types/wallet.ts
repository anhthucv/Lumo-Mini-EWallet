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

export interface TransferRequest {
  recipientAccountNumber: string;
  amount: number;
}

export type TransferResponse = DepositResponse;
