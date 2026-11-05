"use client";

import axios from "axios";

const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080/api/v1";

export const apiClient = axios.create({
  baseURL: API_BASE_URL.replace(/\/+$/, ""),
  withCredentials: true,
  headers: {
    "Content-Type": "application/json",
  },
});

let accessToken = null;
let refreshRequest = null;

export function setAccessToken(token) {
  accessToken = token;
  if (typeof window !== "undefined") {
    window.dispatchEvent(new CustomEvent("auth:token", { detail: token }));
  }
}

export function getAccessToken() {
  return accessToken;
}

export function getCsrfToken() {
  if (typeof document === "undefined") return null;
  const cookie = document.cookie
    .split("; ")
    .find((entry) => entry.startsWith("csrf_token="));
  return cookie ? decodeURIComponent(cookie.slice("csrf_token=".length)) : null;
}

function isSessionEndpoint(url = "") {
  return /\/auth\/(login|refresh|logout)(?:$|[?#])/.test(url);
}

apiClient.interceptors.request.use((config) => {
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }

  if (/\/auth\/(refresh|logout)(?:$|[?#])/.test(config.url || "")) {
    const csrfToken = getCsrfToken();
    if (csrfToken) config.headers["X-CSRF-TOKEN"] = csrfToken;
  }

  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    if (
      error.response?.status !== 401 ||
      !originalRequest ||
      originalRequest._retried ||
      isSessionEndpoint(originalRequest.url)
    ) {
      return Promise.reject(error);
    }

    originalRequest._retried = true;
    try {
      if (!refreshRequest) {
        refreshRequest = axios
          .post(
            `${API_BASE_URL.replace(/\/+$/, "")}/auth/refresh`,
            null,
            {
              withCredentials: true,
            headers: { "X-CSRF-TOKEN": getCsrfToken() || "" },
            },
          )
          .then((response) => {
            const token = response.data?.data?.accessToken;
            if (!token) throw new Error("The refresh response did not contain an access token");
            setAccessToken(token);
            return token;
          })
          .finally(() => {
            refreshRequest = null;
          });
      }

      const token = await refreshRequest;
      originalRequest.headers.Authorization = `Bearer ${token}`;
      return apiClient(originalRequest);
    } catch (refreshError) {
      setAccessToken(null);
      if (typeof window !== "undefined") {
        window.dispatchEvent(new Event("auth:expired"));
      }
      return Promise.reject(refreshError);
    }
  },
);

export function getApiErrorMessage(error, fallback = "The request could not be completed.") {
  return (
    error.response?.data?.message ||
    error.response?.data?.error ||
    error.message ||
    fallback
  );
}
