import { apiClient } from "./apiClient";

export async function getRoles(signal) {
  const response = await apiClient.get("/admin/roles", { signal });
  return response.data.data;
}

export async function getPermissions(signal) {
  const response = await apiClient.get("/admin/permissions", { signal });
  return response.data.data;
}

export async function saveRole(id, role) {
  const response = id
    ? await apiClient.put(`/admin/roles/${id}`, role)
    : await apiClient.post("/admin/roles", role);
  return response.data.data;
}
