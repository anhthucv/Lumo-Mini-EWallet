export interface SendVerificationCodeRequest {
  email: string;
}

export interface VerificationCodeResponse {
  message: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  fullName: string;
  code: string;
}

export interface RegisterResponse {
  userId: number;
  email: string;
  fullName: string;
  accountId: number;
  accountNumber: string;
  balance: number | string;
  role: string;
  userStatus: string;
  accountStatus: string;
}

export interface ApiErrorResponse {
  timestamp?: string;
  status: number;
  error: string;
  message: string;
  path?: string;
}
