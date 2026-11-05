import { apiClient } from "./apiClient";

export async function getInquiries({ page = 0, size = 10, search = "", status, signal } = {}) {
  const response = await apiClient.get("/admin/inquiries", {
    params: { page, size, ...(search ? { search } : {}), ...(status ? { status } : {}) },
    signal,
  });
  return response.data.data;
}

export async function updateInquiryStatus(id, status) {
  const response = await apiClient.patch(`/admin/inquiries/${id}/status`, { status });
  return response.data.data;
}
