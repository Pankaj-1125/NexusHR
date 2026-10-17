import { apiFetch } from "./apiClient";

export async function getDashboardStats() {
  return apiFetch("/dashboard/stats");
}