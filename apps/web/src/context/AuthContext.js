"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { apiClient, getApiErrorMessage, getCsrfToken, setAccessToken } from "@/services/api/apiClient";
import {
  getCurrentUser,
  login as loginRequest,
  logout as logoutRequest,
  registerCustomer as registerCustomerRequest,
} from "@/services/api/authApi";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const router = useRouter();
  const [user, setUser] = useState(null);
  const [status, setStatus] = useState("loading");
  const [authError, setAuthError] = useState("");
  const restoreAttempted = useRef(false);

  const loadCurrentUser = useCallback(async (signal) => {
    const currentUser = await getCurrentUser(signal);
    setUser(currentUser);
    setStatus("authenticated");
    setAuthError("");
    return currentUser;
  }, []);

  useEffect(() => {
    const onToken = (event) => {
      if (event.detail === null) {
        setUser(null);
        setStatus("unauthenticated");
      }
    };
    const onExpired = () => {
      setUser(null);
      setStatus("unauthenticated");
    };
    window.addEventListener("auth:token", onToken);
    window.addEventListener("auth:expired", onExpired);

    return () => {
      window.removeEventListener("auth:token", onToken);
      window.removeEventListener("auth:expired", onExpired);
    };
  }, []);

  useEffect(() => {
    if (restoreAttempted.current) return undefined;
    restoreAttempted.current = true;

    async function restoreSession() {
      if (!getCsrfToken()) {
        setStatus("unauthenticated");
        return;
      }
      try {
        const response = await apiClient.post("/auth/refresh", null);
        const token = response.data?.data?.accessToken;
        if (!token) throw new Error("The refresh response did not contain an access token");
        setAccessToken(token);
        await loadCurrentUser();
      } catch (error) {
        if (error.response?.status === 401) {
          setAccessToken(null);
          setUser(null);
          setStatus("unauthenticated");
        } else {
          setAuthError(getApiErrorMessage(error, "Unable to connect to the authentication service."));
          setStatus("error");
        }
      }
    }

    restoreSession();
    return undefined;
  }, [loadCurrentUser]);

  const signIn = useCallback(async (email, password) => {
    setAuthError("");
    try {
      const result = await loginRequest(email, password);
      if (!result.accessToken || !result.user) {
        throw new Error("The login response was incomplete.");
      }
      setAccessToken(result.accessToken);
      setUser(result.user);
      setStatus("authenticated");
      return result.user;
    } catch (error) {
      setAuthError(getApiErrorMessage(error, "Unable to sign in."));
      throw error;
    }
  }, []);

  const registerCustomer = useCallback(async (account) => {
    setAuthError("");
    try {
      const result = await registerCustomerRequest(account);
      if (!result.accessToken || !result.user) {
        throw new Error("The registration response was incomplete.");
      }
      if (!result.user.roles?.includes("CUSTOMER")) {
        try {
          await logoutRequest();
        } finally {
          setAccessToken(null);
          setUser(null);
          setStatus("unauthenticated");
        }
        throw new Error("The server did not create a customer account.");
      }
      setAccessToken(result.accessToken);
      setUser(result.user);
      setStatus("authenticated");
      return result.user;
    } catch (error) {
      setAuthError(getApiErrorMessage(error, "Unable to create your account."));
      throw error;
    }
  }, []);

  const signOut = useCallback(async () => {
    let logoutError = null;
    try {
      await logoutRequest();
    } catch (error) {
      logoutError = error;
    } finally {
      setAccessToken(null);
      setUser(null);
      setStatus("unauthenticated");
      router.replace("/");
    }
    if (logoutError) throw logoutError;
  }, [router]);

  const value = useMemo(
    () => ({
      user,
      status,
      authError,
      signIn,
      registerCustomer,
      signOut,
      can: (permission) => Boolean(user?.permissions?.includes(permission)),
      isAdministrator: Boolean(
        user?.roles?.some((role) => ["ADMIN", "SUPER_ADMIN"].includes(role)),
      ),
    }),
    [user, status, authError, signIn, registerCustomer, signOut],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used within AuthProvider");
  return context;
}
