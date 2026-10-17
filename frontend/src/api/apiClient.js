export const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || "/api").replace(/\/$/, "");

export function apiUrl(endpoint = "") {
  const path = endpoint.startsWith("/") ? endpoint : `/${endpoint}`;
  return `${API_BASE_URL}${path}`;
}

export async function apiFetch(endpoint, options = {}) {
  let token = "";

  if (typeof window !== "undefined") {
    try {
      const raw = window.localStorage.getItem("nexushr-user");
      const parsed = raw ? JSON.parse(raw) : null;
      token = String(
        parsed?.token || window.localStorage.getItem("nexushr-token") || ""
      ).trim();
    } catch (error) {
      console.error("Unable to read authentication session:", error);
    }
  }

  const headers = {
    Accept: "application/json",
    ...(options.body
      ? {
          "Content-Type": "application/json",
        }
      : {}),
    ...(options.headers || {}),
  };

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(
    apiUrl(endpoint),
    {
      credentials: "include",
      ...options,
      headers,
    }
  );

  if (response.status === 401 || (response.status === 403 && !token)) {
    try {
      window.localStorage.removeItem("nexushr-user");
      window.localStorage.removeItem("nexushr-token");
      window.localStorage.removeItem("nexushr-refresh-token");
    } catch {}

    throw new Error(
      "Your login session is missing or expired. Please sign in again."
    );
  }

  if (response.status === 403) {
    throw new Error(
      "Access denied. Your account does not have permission to access this resource."
    );
  }

  if (!response.ok) {
    let message =
      `Request failed with status ${response.status}.`;

    try {
      const raw = await response.text();

      if (raw) {
        try {
          const data = JSON.parse(raw);
          message = data?.message || data?.error || raw;
        } catch {
          message = raw;
        }
      }
    } catch {
      // Keep the status-based fallback.
    }

    throw new Error(message);
  }

  if (response.status === 204) {
    return null;
  }

  const contentType =
    response.headers.get("content-type") || "";

  if (contentType.includes("application/json")) {
    return response.json();
  }

  return response.text();
}