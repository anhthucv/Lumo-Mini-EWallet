export interface AdminUser {
  userId: number;
  email: string;
  fullName: string;
  role: string;
  status: string;
  createdAt: string;
  accountId: number | null;
  accountNumberSummary: string | null;
  accountStatus: string | null;
  balance: number | string | null;
}

export interface AdminUsersPage {
  content: AdminUser[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}
