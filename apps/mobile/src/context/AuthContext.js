import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import * as SecureStore from "expo-secure-store";
import {
  apiClient,
  clearStoredRefreshToken,
  saveRefreshToken,
  setAccessToken,
  setSessionExpiredHandler,
} from "../services/apiClient";
import {
  getCurrentUser,
  login as loginRequest,
  logout as logoutRequest,
  refreshSession,
  registerCustomer as registerRequest,
  requestPasswordReset,
  resetPassword,
} from "../services/authService";

const AuthContext = createContext(null);
const REFRESH_TOKEN_KEY = "designer_costume_refresh_token";

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [status, setStatus] = useState("loading");
  const [restoreError, setRestoreError] = useState("");

  const clearSession = useCallback(() => {
    setAccessToken(null);
    setUser(null);
    setStatus("unauthenticated");
  }, []);

  useEffect(() => {
    setSessionExpiredHandler(clearSession);
    let active = true;
    async function restore() {
      try {
        setRestoreError("");
        const refreshToken = await SecureStore.getItemAsync(REFRESH_TOKEN_KEY);
        if (!refreshToken) {
          if (active) setStatus("unauthenticated");
          return;
        }
        const session = await refreshSession(refreshToken);
        if (!session?.accessToken || !session?.refreshToken) {
          throw new Error("The session refresh response was incomplete.");
        }
        await saveRefreshToken(session.refreshToken);
        setAccessToken(session.accessToken);
        const currentUser = await getCurrentUser(apiClient);
        if (!currentUser?.roles?.includes("CUSTOMER")) {
          await clearStoredRefreshToken();
          setAccessToken(null);
          if (active) {
            setUser(null);
            setStatus("unauthenticated");
          }
          return;
        }
        if (active) {
          setUser(currentUser);
          setStatus("authenticated");
        }
      } catch (error) {
        setAccessToken(null);
        if (error.response?.status === 401 || error.response?.status === 403) {
          await clearStoredRefreshToken();
          if (active) {
            setUser(null);
            setStatus("unauthenticated");
          }
        } else if (active) {
          setUser(null);
          setRestoreError(error.message || "Could not restore your session.");
          setStatus("error");
        }
      }
    }
    restore();
    return () => {
      active = false;
      setSessionExpiredHandler(null);
    };
  }, [clearSession]);

  const acceptSession = useCallback(async (session) => {
    if (!session?.accessToken || !session?.refreshToken || !session?.user) {
      throw new Error("The authentication response was incomplete.");
    }
    await saveRefreshToken(session.refreshToken);
    setAccessToken(session.accessToken);
    setUser(session.user);
    setStatus("authenticated");
    return session.user;
  }, []);

  const signIn = useCallback(async (email, password) => {
    return acceptSession(await loginRequest(email, password));
  }, [acceptSession]);

  const register = useCallback(async (account) => {
    return acceptSession(await registerRequest(account));
  }, [acceptSession]);

  const signOut = useCallback(async () => {
    let logoutError;
    try {
      const refreshToken = await SecureStore.getItemAsync(REFRESH_TOKEN_KEY);
      if (refreshToken) await logoutRequest(refreshToken);
    } catch (error) {
      logoutError = error;
    } finally {
      clearSession();
      await clearStoredRefreshToken();
    }
    if (logoutError) throw logoutError;
  }, [clearSession]);

  const value = useMemo(() => ({
    user,
    status,
    restoreError,
    signIn,
    register,
    signOut,
    requestReset: requestPasswordReset,
    submitReset: resetPassword,
    can: (role) => Boolean(user?.roles?.includes(role)),
    refreshUser: async () => {
      const currentUser = await getCurrentUser(apiClient);
      setUser(currentUser);
      return currentUser;
    },
  }), [user, status, restoreError, signIn, register, signOut]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used within AuthProvider");
  return context;
}
