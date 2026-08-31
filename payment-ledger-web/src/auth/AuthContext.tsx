import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';

import { clearAuthSession, loadAuthSession, saveAuthSession } from './session';
import type { AuthSession, AuthUser, LoginResponse } from '../types/auth';

export type AuthenticatedUser = AuthUser;

export interface AuthContextValue {
  user: AuthenticatedUser | null;
  accessToken: string | null;
  tokenType: string | null;
  expiresIn: number | null;
  isAuthenticated: boolean;
  isHydrating: boolean;
  login: (session: LoginResponse) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

function normalizeSession(session: AuthSession | null): Pick<
  AuthContextValue,
  'user' | 'accessToken' | 'tokenType' | 'expiresIn' | 'isAuthenticated'
> {
  if (!session) {
    return {
      user: null,
      accessToken: null,
      tokenType: null,
      expiresIn: null,
      isAuthenticated: false,
    };
  }

  return {
    user: session.user,
    accessToken: session.accessToken,
    tokenType: session.tokenType,
    expiresIn: session.expiresIn,
    isAuthenticated: true,
  };
}

function toStoredSession(response: LoginResponse): AuthSession {
  return {
    accessToken: response.accessToken,
    tokenType: response.tokenType,
    expiresIn: response.expiresIn,
    user: {
      userId: response.userId,
      email: response.email,
      fullName: response.fullName,
      role: response.role,
      status: response.status,
    },
  };
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [authState, setAuthState] = useState(() => normalizeSession(null));
  const [isHydrating, setIsHydrating] = useState(true);

  useEffect(() => {
    setAuthState(normalizeSession(loadAuthSession()));
    setIsHydrating(false);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      ...authState,
      isHydrating,
      login: (session: LoginResponse) => {
        const storedSession = toStoredSession(session);
        saveAuthSession(storedSession);
        setAuthState(normalizeSession(storedSession));
        setIsHydrating(false);
      },
      logout: () => {
        clearAuthSession();
        setAuthState(normalizeSession(null));
        setIsHydrating(false);
      },
    }),
    [authState, isHydrating],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider.');
  }

  return context;
}
