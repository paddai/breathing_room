import { useCallback, useEffect, useMemo, useState } from "react";
import { api } from "../api";
import { AuthContext } from "./authContext";
import { clearSession, loadSession, saveSession } from "./authStore";

export default function AuthProvider({ children }) {
  const [user, setUser] = useState(() => loadSession()?.user ?? null);

  // api.js announces "auth:expired" whenever the server says our token is no longer valid
  useEffect(() => {
    const onExpired = () => setUser(null);
    window.addEventListener("auth:expired", onExpired);
    return () => window.removeEventListener("auth:expired", onExpired);
  }, []);

  const finish = useCallback((res) => {
    saveSession({ token: res.token, user: res.user, expiresAt: Date.now() + res.expiresInSeconds * 1000 });
    setUser(res.user);
  }, []);

  const login = useCallback(async (email, password) => finish(await api.post("/auth/login", { email, password })), [finish]);
  const register = useCallback(async (email, password) => finish(await api.post("/auth/register", { email, password })), [finish]);

  const logout = useCallback(() => {
    clearSession();
    setUser(null);
  }, []);

  const deleteAccount = useCallback(async (password) => {
    await api.post("/auth/delete-account", { password });
    clearSession();
    setUser(null);
  }, []);

  const value = useMemo(() => ({ user, login, register, logout, deleteAccount }), [user, login, register, logout, deleteAccount]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
