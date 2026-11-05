import { apiClient } from "./apiClient";

export async function getCostumes({
  page = 0,
  size = 10,
  search = "",
  categoryId,
  signal,
} = {}) {
  const response = await apiClient.get("/costumes", {
    params: {
      page,
      size,
      ...(search ? { search } : {}),
      ...(categoryId ? { categoryId } : {}),
    },
    signal,
  });
  return response.data.data;
}

export async function getCostume(costumeId, signal) {
  const response = await apiClient.get(`/costumes/${costumeId}`, { signal });
  return response.data.data;
}

export async function getCostumeVariants(costumeId, signal) {
  const response = await apiClient.get(`/costumes/${costumeId}/variants`, { signal });
  return response.data.data;
}
