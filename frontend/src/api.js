import { useCallback, useEffect, useState } from "react";
import { clearSession, getToken } from "./auth/authStore";

const NETWORK_MESSAGE = "Cannot reach the server. Is the backend running on port 8080?";

// All calls go to /api/... and Vite's proxy forwards them to Spring Boot (see vite.config.js).
// The signed-in user's token is added to every request automatically.
async function send(path, options = {}) {
  const headers = { ...(options.headers || {}) };
  const token = getToken();
  if (token) headers.Authorization = `Bearer ${token}`;

  let res;
  try {
    res = await fetch(`/api${path}`, { ...options, headers });
  } catch {
    throw new Error(NETWORK_MESSAGE);
  }
  if (res.status === 401 && !path.startsWith("/auth/")) {
    // our token is missing, expired or invalid: sign out so the login page appears
    clearSession();
    window.dispatchEvent(new Event("auth:expired"));
  }
  return res;
}

// Turns an error answer into an Error. If the server named the inputs that are wrong, they are on error.fieldErrors.
async function failure(res) {
  let message = `Something went wrong (error ${res.status}). Please try again.`;
  let fieldErrors = null;
  try {
    const body = await res.json();
    if (body.message) message = body.message;
    fieldErrors = body.fieldErrors || null;
  } catch {
    /* the answer had no JSON body */
  }
  const err = new Error(message);
  err.fieldErrors = fieldErrors;
  err.status = res.status;
  return err;
}

async function request(path, options = {}) {
  const res = await send(path, { ...options, headers: { "Content-Type": "application/json", ...(options.headers || {}) } });
  if (!res.ok) throw await failure(res);
  if (res.status === 204) return null;
  const text = await res.text();
  return text ? JSON.parse(text) : null;
}

export const api = {
  get: (path) => request(path),
  post: (path, body) => request(path, { method: "POST", body: JSON.stringify(body ?? {}) }),
  put: (path, body) => request(path, { method: "PUT", body: JSON.stringify(body ?? {}) }),
  del: (path) => request(path, { method: "DELETE" }),
  // Loads a picture the browser cannot fetch by itself (it needs our token). Returns an object URL, or null if there is none.
  async blobUrl(path) {
    const res = await send(path);
    if (res.status === 404) return null;
    if (!res.ok) throw await failure(res);
    return URL.createObjectURL(await res.blob());
  },
};

// Loads data when a page opens. `loader` must be a stable function (define it outside the component
// or wrap it in useCallback). Call reload() after you change something.
export function useLoad(loader) {
  const [state, setState] = useState({ data: null, error: null, loading: true });

  const reload = useCallback(async () => {
    try {
      const data = await loader();
      setState({ data, error: null, loading: false });
    } catch (e) {
      setState({ data: null, error: e.message, loading: false });
    }
  }, [loader]);

  useEffect(() => {
    reload();
  }, [reload]);

  return { ...state, reload };
}
