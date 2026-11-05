import { apiClient } from "./apiClient";

export async function createAdminUser(user) {
  const response = await apiClient.post("/admin/users", user);
  return response.data.data;
}
