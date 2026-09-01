export interface ProfileResponse {
  userId: number;
  email: string;
  fullName: string;
  role: string;
  status: string;
  createdAt: string | null;
}

export interface UpdateProfileRequest {
  fullName: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}
