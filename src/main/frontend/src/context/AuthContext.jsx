import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import { authApi } from '../api/auth.js';
import { setTokenProvider, setUnauthorizedHandler } from '../api/client.js';

const STORAGE_KEY = 'bridge.token';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem(STORAGE_KEY));
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(Boolean(localStorage.getItem(STORAGE_KEY)));
  const [expired, setExpired] = useState(false);

  // The API client reads the token through a ref, so it always sees the
  // current value without the client module depending on React.
  const tokenRef = useRef(token);
  tokenRef.current = token;

  const clear = useCallback(() => {
    localStorage.removeItem(STORAGE_KEY);
    tokenRef.current = null;
    setToken(null);
    setUser(null);
  }, []);

  useEffect(() => {
    setTokenProvider(() => tokenRef.current);

    // The token can expire with the tab still open; without this every page
    // would report an unrecoverable load failure.
    setUnauthorizedHandler(() => {
      if (!tokenRef.current) return;
      clear();
      setExpired(true);
      setLoading(false);
    });
  }, [clear]);

  // A stored token survives a reload, but it may have expired while the tab
  // was closed, so it is verified against the server before being trusted.
  useEffect(() => {
    if (!token) {
      setUser(null);
      setLoading(false);
      return;
    }

    let cancelled = false;
    authApi
      .me()
      .then((profile) => {
        if (!cancelled) setUser(profile);
      })
      .catch(() => {
        if (!cancelled) clear();
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [token]);

  const adopt = useCallback((response) => {
    localStorage.setItem(STORAGE_KEY, response.token);
    tokenRef.current = response.token;
    setToken(response.token);
    setUser(response.user);
    setExpired(false);
    setLoading(false);
    return response.user;
  }, []);

  const login = useCallback(
    async (credentials) => adopt(await authApi.login(credentials)),
    [adopt],
  );

  const register = useCallback(
    async (payload) => adopt(await authApi.register(payload)),
    [adopt],
  );

  const logout = useCallback(() => {
    clear();
    setExpired(false);
  }, [clear]);

  // Re-reads the account after something about it changes on the server.
  const refresh = useCallback(async () => {
    const profile = await authApi.me();
    setUser(profile);
    return profile;
  }, []);

  const value = useMemo(
    () => ({ user, loading, expired, isAuthenticated: Boolean(user), login, register, logout, refresh }),
    [user, loading, expired, login, register, logout, refresh],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth doit être utilisé à l\'intérieur d\'un AuthProvider.');
  }
  return context;
}
