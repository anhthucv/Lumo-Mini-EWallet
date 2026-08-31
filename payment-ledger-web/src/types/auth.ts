export interface SendVerificationCodeRequest {
  email: string;
}

export interface VerificationCodeResponse {
  message: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthUser {
  userId: number;
  email: string;
  fullName: string;
  role: string;
  status: string;
}

export interface LoginResponse extends AuthUser {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface RegisterRequest {
  email: string;
  password: string;
  fullName: string;
  code: string;
}

export interface RegisterResponse extends AuthUser {
  accountId: number;
  accountNumber: string;
  balance: number | string;
  userStatus: string;
  accountStatus: string;
}

export interface AuthSession {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: AuthUser;
}

export interface ApiErrorResponse {
  timestamp?: string;
  status: number;
  error: string;
  message: string;
  path?: string;
}
