const USER_KEY = "nexushr-user";
const TOKEN_KEY = "nexushr-token";
const REFRESH_TOKEN_KEY = "nexushr-refresh-token";

export function normalizeRole(role) {
  const raw = String(role || "").trim().toUpperCase();
  if (!raw) return "";
  return raw.startsWith("ROLE_") ? raw : `ROLE_${raw}`;
}

export function saveAuthSession(data) {
  if (!data || typeof data !== "object") {
    throw new Error("Invalid login response.");
  }

  const token = String(data.token || "").trim();
  if (!token) {
    throw new Error("Login succeeded but the server did not return an access token.");
  }

  const session = {
    ...data,
    role: normalizeRole(data.role),
    token,
  };

  if (typeof window === "undefined") return session;

  window.localStorage.setItem(USER_KEY, JSON.stringify(session));
  window.localStorage.setItem(TOKEN_KEY, token);

  if (data.refreshToken) {
    window.localStorage.setItem(REFRESH_TOKEN_KEY, String(data.refreshToken));
  } else {
    window.localStorage.removeItem(REFRESH_TOKEN_KEY);
  }

  return session;
}

export function loadAuthSession() {
  if (typeof window === "undefined") return null;

  try {
    const raw = window.localStorage.getItem(USER_KEY);
    const fallbackToken = window.localStorage.getItem(TOKEN_KEY);

    if (!raw) return null;

    const parsed = JSON.parse(raw);
    const token = String(parsed?.token || fallbackToken || "").trim();

    if (!token) {
      clearAuthSession();
      return null;
    }

    return {
      ...parsed,
      role: normalizeRole(parsed?.role),
      token,
    };
  } catch {
    clearAuthSession();
    return null;
  }
}

export function getAccessToken() {
  const session = loadAuthSession();
  return session?.token || "";
}

export function getRefreshToken() {
  if (typeof window === "undefined") return "";
  return String(
    window.localStorage.getItem(REFRESH_TOKEN_KEY) || ""
  ).trim();
}

export function hasAuthSession() {
  return Boolean(loadAuthSession());
}

export function clearAuthSession() {
  if (typeof window === "undefined") return;
  window.localStorage.removeItem(USER_KEY);
  window.localStorage.removeItem(TOKEN_KEY);
  window.localStorage.removeItem(REFRESH_TOKEN_KEY);
}
