import { useCallback, useEffect, useMemo, useState } from "react";
import { api } from "../api";
import { AuthContext } from "./authContext";
import { clearSession, loadSession, saveSession } from "./authStore";

export default function AuthProvider({ children }) {
  const [user, setUser] = useState(() => loadSession()?.user ?? null);
  const [profile, setProfile] = useState(null); // { displayName, hasAvatar, ... }
  const [avatarUrl, setAvatarUrl] = useState(null);

  const forget = useCallback(() => {
    setUser(null);
    setProfile(null);
    setAvatarUrl((old) => {
      if (old) URL.revokeObjectURL(old);
      return null;
    });
  }, []);

  // api.js announces "auth:expired" whenever the server says our token is no longer valid
  useEffect(() => {
    window.addEventListener("auth:expired", forget);
    return () => window.removeEventListener("auth:expired", forget);
  }, [forget]);

  // The profile (display name and picture) is nice to have, so a failure here is ignored.
  const refreshProfile = useCallback(async () => {
    try {
      const p = await api.get("/profile");
      const url = p.hasAvatar ? await api.blobUrl("/profile/avatar") : null;
      setProfile(p);
      setAvatarUrl((old) => {
        if (old) URL.revokeObjectURL(old);
        return url;
      });
    } catch {
      /* keep what we have */
    }
  }, []);

  useEffect(() => {
    if (user) refreshProfile();
  }, [user, refreshProfile]);

  const finish = useCallback((res) => {
    saveSession({ token: res.token, user: res.user, expiresAt: Date.now() + res.expiresInSeconds * 1000 });
    setUser(res.user);
  }, []);

  const login = useCallback(async (email, password) => finish(await api.post("/auth/login", { email, password })), [finish]);
  const register = useCallback(async (email, password) => finish(await api.post("/auth/register", { email, password })), [finish]);

  const logout = useCallback(() => {
    clearSession();
    forget();
  }, [forget]);

  const deleteAccount = useCallback(
    async (password) => {
      await api.post("/auth/delete-account", { password });
      clearSession();
      forget();
    },
    [forget]
  );

  const value = useMemo(
    () => ({ user, profile, avatarUrl, refreshProfile, login, register, logout, deleteAccount }),
    [user, profile, avatarUrl, refreshProfile, login, register, logout, deleteAccount]
  );
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
