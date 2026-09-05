const PENDING_TOP_UP_KEY = 'lumo.pendingTopUp';

export interface PendingTopUpContext {
  topUpId: number;
  merchantOrderCode: number;
}

export function savePendingTopUp(context: PendingTopUpContext): void {
  try {
    sessionStorage.setItem(PENDING_TOP_UP_KEY, JSON.stringify(context));
  } catch {
    // A blocked session storage must never be treated as payment state.
  }
}

export function loadPendingTopUp(): PendingTopUpContext | null {
  try {
    const raw = sessionStorage.getItem(PENDING_TOP_UP_KEY);
    if (!raw) return null;
    const parsed: unknown = JSON.parse(raw);
    if (!parsed || typeof parsed !== 'object') return null;
    const value = parsed as Record<string, unknown>;
    return typeof value.topUpId === 'number' && Number.isSafeInteger(value.topUpId)
      && value.topUpId > 0
      && typeof value.merchantOrderCode === 'number'
      && Number.isSafeInteger(value.merchantOrderCode)
      && value.merchantOrderCode > 0
      ? { topUpId: value.topUpId, merchantOrderCode: value.merchantOrderCode }
      : null;
  } catch {
    return null;
  }
}

export function clearPendingTopUp(): void {
  try {
    sessionStorage.removeItem(PENDING_TOP_UP_KEY);
  } catch {
    // Storage cleanup is best effort and not payment state.
  }
}
