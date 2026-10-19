export const API_BASE_URL = (
  import.meta.env.VITE_API_BASE_URL || "/api"
).replace(/\/$/, "");

const DEFAULT_TIMEOUT_MS = 20000;

export function apiUrl(endpoint = "") {
  const path = endpoint.startsWith("/")
    ? endpoint
    : `/${endpoint}`;

  return `${API_BASE_URL}${path}`;
}

function readAccessToken() {
  if (typeof window === "undefined") {
    return "";
  }

  try {
    const raw = window.localStorage.getItem("nexushr-user");
    const parsed = raw ? JSON.parse(raw) : null;

    return String(
      parsed?.token ||
        window.localStorage.getItem("nexushr-token") ||
        ""
    ).trim();
  } catch (error) {
    console.error("Unable to read authentication session:", error);
    return "";
  }
}

function clearStoredSession() {
  if (typeof window === "undefined") {
    return;
  }

  try {
    window.localStorage.removeItem("nexushr-user");
    window.localStorage.removeItem("nexushr-token");
    window.localStorage.removeItem("nexushr-refresh-token");
  } catch {
    // Ignore storage errors.
  }
}

export async function apiFetch(endpoint, options = {}) {
  const {
    timeoutMs = DEFAULT_TIMEOUT_MS,
    headers: optionHeaders,
    signal: callerSignal,
    ...fetchOptions
  } = options;

  const token = readAccessToken();

  const headers = {
    Accept: "application/json",
    ...(fetchOptions.body
      ? { "Content-Type": "application/json" }
      : {}),
    ...(optionHeaders || {}),
  };

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const controller = new AbortController();
  let timeoutId = null;

  const abortFromCaller = () => controller.abort();

  if (callerSignal) {
    if (callerSignal.aborted) {
      controller.abort();
    } else {
      callerSignal.addEventListener("abort", abortFromCaller, { once: true });
    }
  }

  if (timeoutMs > 0) {
    timeoutId = window.setTimeout(() => {
      controller.abort();
    }, timeoutMs);
  }

  try {
    const response = await fetch(apiUrl(endpoint), {
      credentials: "include",
      ...fetchOptions,
      headers,
      signal: controller.signal,
    });

    if (
      response.status === 401 ||
      (response.status === 403 && !token)
    ) {
      clearStoredSession();

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
      let message = `Request failed with status ${response.status}.`;

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
        // Keep status fallback.
      }

      throw new Error(message);
    }

    if (response.status === 204) {
      return null;
    }

    const contentType = response.headers.get("content-type") || "";

    if (contentType.includes("application/json")) {
      return response.json();
    }

    return response.text();
  } catch (error) {
    if (error?.name === "AbortError") {
      throw new Error(
        "The server is taking longer than expected. Please try again in a few seconds."
      );
    }

    throw error;
  } finally {
    if (timeoutId !== null) {
      window.clearTimeout(timeoutId);
    }

    if (callerSignal) {
      callerSignal.removeEventListener("abort", abortFromCaller);
    }
  }
}

export async function warmBackend(timeoutMs = 5000) {
  const controller = new AbortController();

  const timeoutId = window.setTimeout(() => {
    controller.abort();
  }, timeoutMs);

  try {
    await fetch(apiUrl("/health"), {
      method: "GET",
      cache: "no-store",
      signal: controller.signal,
    });
  } catch {
    // This is intentionally silent. Its purpose is only to trigger a wake-up.
  } finally {
    window.clearTimeout(timeoutId);
  }
}
