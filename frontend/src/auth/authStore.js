// Keeps the signed-in session in sessionStorage: it disappears when the browser tab closes.
// Trade-off (documented in SECURITY.md): any script running on the page could read it, so never add untrusted scripts.
const KEY = "breathingroom.session";

export function loadSession() {
  try {
    const raw = sessionStorage.getItem(KEY);
    if (!raw) return null;
    const session = JSON.parse(raw);
    if (!session.token || !session.expiresAt || Date.now() >= session.expiresAt) {
      sessionStorage.removeItem(KEY);
      return null;
    }
    return session;
  } catch {
    return null;
  }
}

export function saveSession(session) {
  try {
    sessionStorage.setItem(KEY, JSON.stringify(session));
  } catch {
    /* storage unavailable: the user will simply have to sign in again after a refresh */
  }
}

export function clearSession() {
  try {
    sessionStorage.removeItem(KEY);
  } catch {
    /* nothing to clear */
  }
}

export function getToken() {
  return loadSession()?.token ?? null;
}
