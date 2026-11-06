import axios from "axios";
import * as SecureStore from "expo-secure-store";

const API_BASE_URL = process.env.EXPO_PUBLIC_API_BASE_URL || "http://10.0.2.2:8080/api/v1";
const REFRESH_TOKEN_KEY = "designer_costume_refresh_token";

export const rawApiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: { Accept: "application/json", "Content-Type": "application/json" },
});

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: { Accept: "application/json", "Content-Type": "application/json" },
});

let accessToken = null;
let refreshPromise = null;
let onSessionExpired = () => {};

export function setAccessToken(token) {
  accessToken = token || null;
}

export function getAccessToken() {
  return accessToken;
}

export function setSessionExpiredHandler(handler) {
  onSessionExpired = handler || (() => {});
}

export async function saveRefreshToken(token) {
  if (!token) throw new Error("The authentication response did not contain a refresh token.");
  await SecureStore.setItemAsync(REFRESH_TOKEN_KEY, token);
}

export async function clearStoredRefreshToken() {
  await SecureStore.deleteItemAsync(REFRESH_TOKEN_KEY);
}

async function rotateAccessToken() {
  if (!refreshPromise) {
    refreshPromise = (async () => {
      const refreshToken = await SecureStore.getItemAsync(REFRESH_TOKEN_KEY);
      if (!refreshToken) throw new Error("Your session has expired. Please sign in again.");
      const response = await rawApiClient.post(
        "/auth/refresh",
        { refreshToken },
        { headers: { "X-Client-Platform": "mobile" } },
      );
      const data = response.data?.data;
      if (!data?.accessToken || !data?.refreshToken) {
        throw new Error("The session refresh response was incomplete.");
      }
      await saveRefreshToken(data.refreshToken);
      setAccessToken(data.accessToken);
      return data.accessToken;
    })()
      .catch(async (error) => {
        if (error.response?.status === 401 || error.response?.status === 403) {
          setAccessToken(null);
          await clearStoredRefreshToken();
          onSessionExpired();
        }
        throw error;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
}

apiClient.interceptors.request.use((config) => {
  config.headers.set("X-Client-Platform", "mobile");
  if (accessToken) config.headers.set("Authorization", "Bearer " + accessToken);
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const request = error.config;
    if (error.response?.status !== 401 || !request || request._retry) {
      return Promise.reject(error);
    }
    request._retry = true;
    try {
      const token = await rotateAccessToken();
      request.headers.set("Authorization", "Bearer " + token);
      request.headers.set("X-Client-Platform", "mobile");
      return apiClient(request);
    } catch (refreshError) {
      return Promise.reject(refreshError);
    }
  },
);

export function getApiErrorMessage(error, fallback = "The service is temporarily unavailable.") {
  return error?.response?.data?.message || error?.message || fallback;
}

export function getApiBaseUrl() {
  return API_BASE_URL;
}
