import { apiClient } from "./apiClient";

export async function getDashboardSummary(signal) {
  const response = await apiClient.get("/admin/dashboard", { signal });
  return response.data.data;
}
