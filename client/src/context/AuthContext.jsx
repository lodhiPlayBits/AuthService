import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import {
  setAccessToken,
  setSessionHint,
  hasSessionHint,
  bootstrapSession,
  registerSessionListener,
  logoutSession,
  getTokenExpiryMs,
  refreshSession,
} from '../utils/api';

const AuthContext = createContext();

export const useAuth = () => useContext(AuthContext);

const ROTATE_LEAD_MS = 60_000; // rotate this long before the access token expires
const ROTATE_RETRY_MS = 10_000; // retry cadence after a transient rotation failure
const MAX_TIMEOUT_MS = 2_147_483_647; // setTimeout ceiling

const mapUser = (data) => {
  if (!data) return null;
  return {
    id: data.id,
    username: data.username || data.name,
    name: data.name,
    email: data.email,
    phoneNumber: data.phoneNumber,
    gender: data.gender,
    image: data.image,
    provider: data.provider,
    role: data.roles?.[0]?.roleName || data.role || 'USER',
    profileComplete: data.profileComplete,
  };
};

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(null);
  // No hint means no server session to restore → render immediately.
  const [sessionReady, setSessionReady] = useState(() => !hasSessionHint());
  const [rotationTick, setRotationTick] = useState(0);

  // Restore the session on load. /refresh only happens when the hint says a
  // refresh cookie should exist; the data lands via the 'refreshed' event.
  useEffect(() => {
    // One-time scrub of credentials written by the pre-session-engine client.
    try {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
    } catch {
      // Best effort only.
    }

    let cancelled = false;

    const unsubscribe = registerSessionListener((event) => {
      if (cancelled) return;
      if (event.type === 'refreshed') {
        setAccessToken(event.data.accessToken);
        setToken(event.data.accessToken);
        setUser(mapUser(event.data.user));
      } else if (event.type === 'expired') {
        setAccessToken(null);
        setToken(null);
        setUser(null);
      }
    });

    bootstrapSession().finally(() => {
      if (!cancelled) setSessionReady(true);
    });

    return () => {
      cancelled = true;
      unsubscribe();
    };
  }, []);

  // Rotate the access token shortly before it expires.
  useEffect(() => {
    if (!sessionReady || !token) return undefined;
    const expiryMs = getTokenExpiryMs(token);
    if (!expiryMs) return undefined;

    const delay = Math.min(
      Math.max(expiryMs - Date.now() - ROTATE_LEAD_MS, ROTATE_RETRY_MS),
      MAX_TIMEOUT_MS
    );

    const timer = setTimeout(async () => {
      const refreshed = await refreshSession();
      if (!refreshed) {
        // Dead session (state already cleared) or transient failure; bumping
        // re-arms a short retry in the latter case only.
        setRotationTick((n) => n + 1);
      }
    }, delay);

    return () => clearTimeout(timer);
  }, [sessionReady, token, rotationTick]);

  const login = useCallback((userData, jwtToken) => {
    setAccessToken(jwtToken);
    setSessionHint(true);
    setToken(jwtToken);
    setUser(mapUser(userData));
  }, []);

  const logout = useCallback(async () => {
    // Clear local state first so the UI reacts immediately; the server call
    // revokes the refresh token family in the background. The access token is
    // captured first because the gateway requires a Bearer header on /logout.
    const revokeToken = token;
    setAccessToken(null);
    setSessionHint(false);
    setToken(null);
    setUser(null);
    await logoutSession(revokeToken);
  }, [token]);

  const updateUser = useCallback((newUserData) => {
    setUser((prev) => (prev ? { ...prev, ...newUserData } : prev));
  }, []);

  return (
    <AuthContext.Provider
      value={{ user, token, login, logout, updateUser, isAuthenticated: !!user, sessionReady }}
    >
      {children}
    </AuthContext.Provider>
  );
};
