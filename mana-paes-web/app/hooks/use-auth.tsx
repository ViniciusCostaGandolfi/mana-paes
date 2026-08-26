import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import type { UserResponse } from "~/types/api";
import * as authService from "~/services/auth.service";
import { getApiErrorMessage } from "~/lib/api";
import {
  clearTokens,
  getStoredUser,
  setStoredUser,
  setTokens,
} from "~/lib/auth";

export interface AuthContextValue {
  user: UserResponse | null;
  isAuthenticated: boolean;
  isAdmin: boolean;
  isRequester: boolean;
  isProduction: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
  setUser: (user: UserResponse | null) => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUserState] = useState<UserResponse | null>(() => getStoredUser());

  const login = useCallback(async (email: string, password: string) => {
    try {
      const response = await authService.login(email, password);
      setTokens(response.accessToken, response.refreshToken);
      setStoredUser(response.user);
      setUserState(response.user);
    } catch (error) {
      throw new Error(getApiErrorMessage(error));
    }
  }, []);

  const logout = useCallback(() => {
    clearTokens();
    setStoredUser(null);
    setUserState(null);
  }, []);

  const setUser = useCallback((next: UserResponse | null) => {
    setStoredUser(next);
    setUserState(next);
  }, []);

  const value = useMemo<AuthContextValue>(() => {
    const role = user?.role;
    return {
      user,
      isAuthenticated: !!user,
      isAdmin: role === "ROLE_ADMIN",
      isRequester: role === "ROLE_REQUESTER",
      isProduction: role === "ROLE_PRODUCTION",
      login,
      logout,
      setUser,
    };
  }, [user, login, logout, setUser]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth deve ser usado dentro de <AuthProvider>.");
  }
  return ctx;
}