import { useCallback, useEffect, useState } from "react";

// All calls go to /api/... and Vite's proxy forwards them to Spring Boot (see vite.config.js).
async function request(path, options = {}) {
  const res = await fetch(`/api${path}`, {
    headers: { "Content-Type": "application/json" },
    ...options,
  });
  if (!res.ok) {
    let message = `Request failed (${res.status})`;
    try {
      const body = await res.json();
      message = body.message || body.error || message;
    } catch {
      /* response had no JSON body */
    }
    throw new Error(message);
  }
  if (res.status === 204) return null;
  const text = await res.text();
  return text ? JSON.parse(text) : null;
}

export const api = {
  get: (path) => request(path),
  post: (path, body) => request(path, { method: "POST", body: JSON.stringify(body ?? {}) }),
  put: (path, body) => request(path, { method: "PUT", body: JSON.stringify(body ?? {}) }),
  del: (path) => request(path, { method: "DELETE" }),
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
