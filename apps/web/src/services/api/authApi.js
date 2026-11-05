import { apiClient } from "./apiClient";

export async function login(email, password) {
  const response = await apiClient.post("/auth/login", { email, password });
  return response.data.data;
}

export async function registerCustomer(account) {
  const response = await apiClient.post("/auth/register", account);
  return response.data.data;
}

export async function getCurrentUser(signal) {
  const response = await apiClient.get("/auth/me", { signal });
  return response.data.data;
}

export async function logout() {
  const response = await apiClient.post("/auth/logout");
  return response.data;
}
