import { apiClient } from "./apiClient";

export async function getCustomerPhotos(signal) {
  const response = await apiClient.get("/customer/photos", { signal });
  return response.data.data;
}

export async function uploadCustomerPhoto(file) {
  const form = new FormData();
  form.append("file", file);
  const response = await apiClient.post("/customer/photos", form, {
    headers: { "Content-Type": "multipart/form-data" },
    timeout: 60000,
  });
  return response.data.data;
}

export async function getCustomerPhotoContent(photoId) {
  const response = await apiClient.get(`/customer/photos/${photoId}/content`, {
    responseType: "blob",
  });
  return response.data;
}

export async function deleteCustomerPhoto(photoId) {
  await apiClient.delete(`/customer/photos/${photoId}`);
}
