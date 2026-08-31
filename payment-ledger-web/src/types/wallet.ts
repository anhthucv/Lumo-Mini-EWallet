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
