import { apiClient } from "./apiClient";

export async function getCustomerPhotos() {
  const response = await apiClient.get("/customer/photos");
  return response.data.data;
}

export async function uploadCustomerPhoto(asset) {
  const data = new FormData();
  data.append("file", {
    uri: asset.uri,
    name: asset.fileName || asset.uri.split("/").pop() || "photo",
    type: asset.mimeType,
  });
  const response = await apiClient.post("/customer/photos", data, {
    headers: { "Content-Type": "multipart/form-data" },
    timeout: 60000,
  });
  return response.data.data;
}

export async function deleteCustomerPhoto(photoId) {
  await apiClient.delete(`/customer/photos/${photoId}`);
}
