import { apiClient } from "./apiClient";

async function getData(request) {
  return (await request).data.data;
}

export function getCategories({ page = 0, size = 100 } = {}) {
  return getData(apiClient.get("/categories", { params: { page, size } }));
}

export function getCostumes({ page = 0, size = 12, search = "", categoryId } = {}) {
  return getData(apiClient.get("/costumes", {
    params: { page, size, ...(search ? { search } : {}), ...(categoryId ? { categoryId } : {}) },
  }));
}

export function getCostume(id) {
  return getData(apiClient.get(`/costumes/${id}`));
}

export function getCostumeImages(id) {
  return getData(apiClient.get(`/costumes/${id}/images`));
}

export function getCostumeVariants(id) {
  return getData(apiClient.get(`/costumes/${id}/variants`));
}
