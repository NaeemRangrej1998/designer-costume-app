import { rawApiClient } from "./apiClient";

const mobileHeaders = { "X-Client-Platform": "mobile" };

function dataOf(response) {
  return response.data.data;
}

export async function login(email, password) {
  return dataOf(await rawApiClient.post("/auth/login", { email, password }, { headers: mobileHeaders }));
}

export async function registerCustomer(account) {
  return dataOf(await rawApiClient.post("/auth/register", account, { headers: mobileHeaders }));
}

export async function refreshSession(refreshToken) {
  return dataOf(await rawApiClient.post(
    "/auth/refresh",
    { refreshToken },
    { headers: mobileHeaders },
  ));
}

export async function logout(refreshToken) {
  return dataOf(await rawApiClient.post(
    "/auth/logout",
    { refreshToken },
    { headers: mobileHeaders },
  ));
}

export async function getCurrentUser(apiClient) {
  return dataOf(await apiClient.get("/auth/me"));
}

export async function requestPasswordReset(email) {
  return dataOf(await rawApiClient.post(
    "/auth/forgot-password",
    { email },
    { headers: mobileHeaders },
  ));
}

export async function resetPassword(token, newPassword) {
  return dataOf(await rawApiClient.post(
    "/auth/reset-password",
    { token, newPassword },
    { headers: mobileHeaders },
  ));
}
