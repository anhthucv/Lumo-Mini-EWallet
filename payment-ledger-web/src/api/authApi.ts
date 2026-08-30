import type {
  RegisterRequest,
  RegisterResponse,
  SendVerificationCodeRequest,
  VerificationCodeResponse,
} from '../types/auth';
import { requestJson } from './http';

export function sendRegistrationCode(
  payload: SendVerificationCodeRequest,
): Promise<VerificationCodeResponse> {
  return requestJson<VerificationCodeResponse>('/api/auth/register/send-code', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function registerUser(payload: RegisterRequest): Promise<RegisterResponse> {
  return requestJson<RegisterResponse>('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}
