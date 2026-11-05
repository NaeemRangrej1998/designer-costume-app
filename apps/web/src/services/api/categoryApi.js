import { apiClient } from "./apiClient";

export async function getCategories({ page = 0, size = 10, search = "", signal } = {}) {
  const response = await apiClient.get("/categories", {
    params: { page, size, ...(search ? { search } : {}) },
    signal,
  });
  return response.data.data;
}
