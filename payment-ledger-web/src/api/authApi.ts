import type {
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  RegisterResponse,
  SendVerificationCodeRequest,
  VerificationCodeResponse,
} from '../types/auth';
import { requestJson } from './http';

export function sendRegistrationCode(
  payload: SendVerificationCodeRequest,
): Promise<VerificationCodeResponse> {
  return requestJson<VerificationCodeResponse>('/auth/register/send-code', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function registerUser(payload: RegisterRequest): Promise<RegisterResponse> {
  return requestJson<RegisterResponse>('/auth/register', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function loginUser(payload: LoginRequest): Promise<LoginResponse> {
  return requestJson<LoginResponse>('/auth/login', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}
