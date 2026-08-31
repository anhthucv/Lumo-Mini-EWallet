import type { AuthSession, AuthUser } from '../types/auth';

const ACCESS_TOKEN_KEY = 'paymentLedger.accessToken';
const TOKEN_TYPE_KEY = 'paymentLedger.tokenType';
const EXPIRES_IN_KEY = 'paymentLedger.expiresIn';
const USER_KEY = 'paymentLedger.user';

function readStorage(): Storage | null {
  if (typeof window === 'undefined') {
    return null;
  }
  return window.localStorage;
}

function parseJson<T>(value: string | null): T | null {
  if (!value) {
    return null;
  }

  try {
    return JSON.parse(value) as T;
  } catch {
    return null;
  }
}

export function saveAuthSession(session: AuthSession): void {
  const storage = readStorage();
  if (!storage) {
    return;
  }

  storage.setItem(ACCESS_TOKEN_KEY, session.accessToken);
  storage.setItem(TOKEN_TYPE_KEY, session.tokenType);
  storage.setItem(EXPIRES_IN_KEY, String(session.expiresIn));
  storage.setItem(USER_KEY, JSON.stringify(session.user));
}

export function loadAuthSession(): AuthSession | null {
  const storage = readStorage();
  if (!storage) {
    return null;
  }

  const accessToken = storage.getItem(ACCESS_TOKEN_KEY);
  const tokenType = storage.getItem(TOKEN_TYPE_KEY) ?? 'Bearer';
  const expiresInRaw = storage.getItem(EXPIRES_IN_KEY);
  const user = parseJson<AuthUser>(storage.getItem(USER_KEY));

  if (!accessToken || !user || !expiresInRaw) {
    return null;
  }

  const expiresIn = Number(expiresInRaw);
  if (Number.isNaN(expiresIn)) {
    return null;
  }

  return {
    accessToken,
    tokenType,
    expiresIn,
    user,
  };
}

export function clearAuthSession(): void {
  const storage = readStorage();
  if (!storage) {
    return;
  }

  storage.removeItem(ACCESS_TOKEN_KEY);
  storage.removeItem(TOKEN_TYPE_KEY);
  storage.removeItem(EXPIRES_IN_KEY);
  storage.removeItem(USER_KEY);
}

export function getAuthHeaders(): HeadersInit {
  const session = loadAuthSession();
  if (!session?.accessToken) {
    return {};
  }

  return {
    Authorization: `${session.tokenType || 'Bearer'} ${session.accessToken}`,
  };
}

export function hasAuthSession(): boolean {
  return loadAuthSession() !== null;
}
