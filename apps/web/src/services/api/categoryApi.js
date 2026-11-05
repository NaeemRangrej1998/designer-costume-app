import { apiClient } from "./apiClient";

export async function getCategories({ page = 0, size = 10, search = "", signal } = {}) {
  const response = await apiClient.get("/categories", {
    params: { page, size, ...(search ? { search } : {}) },
    signal,
  });
  return response.data.data;
}

export async function getAdminCategories({
  page = 0,
  size = 10,
  search = "",
  active,
  signal,
} = {}) {
  const response = await apiClient.get("/admin/categories", {
    params: { page, size, ...(search ? { search } : {}), ...(active !== undefined ? { active } : {}) },
    signal,
  });
  return response.data.data;
}

export async function getAdminCategory(id, signal) {
  const response = await apiClient.get(`/admin/categories/${id}`, { signal });
  return response.data.data;
}

export async function createCategory(category) {
  const response = await apiClient.post("/admin/categories", category);
  return response.data.data;
}

export async function updateCategory(id, category) {
  const response = await apiClient.put(`/admin/categories/${id}`, category);
  return response.data.data;
}

export async function deactivateCategory(id) {
  const response = await apiClient.delete(`/admin/categories/${id}`);
  return response.data.data;
}
