import { apiClient } from "./apiClient";

export async function createAdminUser(user) {
  const response = await apiClient.post("/admin/users", user);
  return response.data.data;
}

export async function getAdminUsers({ page = 0, size = 10, search = "", active, signal } = {}) {
  const response = await apiClient.get("/admin/users", {
    params: { page, size, ...(search ? { search } : {}), ...(active !== undefined ? { active } : {}) },
    signal,
  });
  return response.data.data;
}

export async function getAdminUser(id, signal) {
  const response = await apiClient.get(`/admin/users/${id}`, { signal });
  return response.data.data;
}

export async function updateAdminUser(id, user) {
  const response = await apiClient.put(`/admin/users/${id}`, user);
  return response.data.data;
}

export async function deactivateAdminUser(id) {
  const response = await apiClient.delete(`/admin/users/${id}`);
  return response.data.data;
}
