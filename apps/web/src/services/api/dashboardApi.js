import { apiClient } from "./apiClient";

export async function getDashboardSummary(signal) {
  const response = await apiClient.get("/admin/dashboard", { signal });
  return response.data.data;
}

export async function getRecentAdminActivity(signal, limit = 5) {
  const response = await apiClient.get("/admin/activity", {
    params: { limit },
    signal,
  });
  return response.data.data;
}
