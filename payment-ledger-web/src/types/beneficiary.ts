export interface Beneficiary {
  id: number;
  accountNumber: string;
  recipientOwnerName: string;
  nickname: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateBeneficiaryRequest {
  accountNumber: string;
  nickname: string;
}

export interface UpdateBeneficiaryRequest {
  nickname: string;
}
