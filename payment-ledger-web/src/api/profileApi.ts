import type { ChangePasswordRequest, ProfileResponse, UpdateProfileRequest } from '../types/profile';
import { requestJson } from './http';

export function getMyProfile(signal?: AbortSignal): Promise<ProfileResponse> {
  return requestJson<ProfileResponse>('/users/me', {
    method: 'GET',
    signal,
  });
}

export function updateMyProfile(
  payload: UpdateProfileRequest,
  signal?: AbortSignal,
): Promise<ProfileResponse> {
  return requestJson<ProfileResponse>('/users/me', {
    method: 'PUT',
    body: JSON.stringify(payload),
    signal,
  });
}

export function changeMyPassword(payload: ChangePasswordRequest, signal?: AbortSignal): Promise<void> {
  return requestJson<void>('/users/me/password', {
    method: 'PUT',
    body: JSON.stringify(payload),
    signal,
  });
}
