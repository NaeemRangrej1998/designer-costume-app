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

export async function getAdminCostumes({
  page = 0,
  size = 10,
  search = "",
  categoryId,
  active,
  signal,
} = {}) {
  const response = await apiClient.get("/admin/costumes", {
    params: {
      page,
      size,
      ...(search ? { search } : {}),
      ...(categoryId ? { categoryId } : {}),
      ...(active !== undefined ? { active } : {}),
    },
    signal,
  });
  return response.data.data;
}

export async function getAdminCostume(id, signal) {
  const response = await apiClient.get(`/admin/costumes/${id}`, { signal });
  return response.data.data;
}

export async function createCostume(costume) {
  const response = await apiClient.post("/admin/costumes", costume);
  return response.data.data;
}

export async function updateCostume(id, costume) {
  const response = await apiClient.put(`/admin/costumes/${id}`, costume);
  return response.data.data;
}

export async function deactivateCostume(id) {
  const response = await apiClient.delete(`/admin/costumes/${id}`);
  return response.data.data;
}

export async function getAdminCostumeVariants(costumeId, signal) {
  const response = await apiClient.get(`/admin/costumes/${costumeId}/variants`, { signal });
  return response.data.data;
}

export async function saveCostumeVariant(costumeId, variantId, variant) {
  const response = variantId
    ? await apiClient.put(`/admin/costumes/${costumeId}/variants/${variantId}`, variant)
    : await apiClient.post(`/admin/costumes/${costumeId}/variants`, variant);
  return response.data.data;
}

export async function getColors(signal) {
  const response = await apiClient.get("/admin/colors", { signal });
  return response.data.data;
}

export async function saveColor(id, color) {
  const response = id
    ? await apiClient.put(`/admin/colors/${id}`, color)
    : await apiClient.post("/admin/colors", color);
  return response.data.data;
}

export async function getSizes(signal) {
  const response = await apiClient.get("/admin/sizes", { signal });
  return response.data.data;
}

export async function saveSize(id, size) {
  const response = id
    ? await apiClient.put(`/admin/sizes/${id}`, size)
    : await apiClient.post("/admin/sizes", size);
  return response.data.data;
}
