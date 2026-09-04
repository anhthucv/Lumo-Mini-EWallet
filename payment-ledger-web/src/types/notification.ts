export type NotificationType =
  | 'DEPOSIT_SUCCESS'
  | 'WITHDRAW_SUCCESS'
  | 'TRANSFER_SENT'
  | 'TRANSFER_RECEIVED';

export interface Notification {
  id: number;
  type: NotificationType;
  title: string;
  message: string;
  amount: number | string | null;
  transactionReference: string | null;
  read: boolean;
  readAt: string | null;
  createdAt: string;
}

export interface NotificationPage {
  content: Notification[];
  number: number;
  size: number;
  totalPages: number;
  totalElements: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface UnreadNotificationCount {
  unreadCount: number;
}

export interface MarkAllNotificationsReadResponse {
  updatedCount: number;
}
