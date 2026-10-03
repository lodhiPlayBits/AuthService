/**
 * API utility + session engine for the SPA.
 *
 * Access token: memory only (module scope, mirrored into React state).
 * Refresh token: HttpOnly cookie owned by auth-service — JS never sees it.
 * Refresh rotation uses strict reuse detection (a replayed refresh token
 * revokes the whole family), so refreshes are single-flight in-tab and
 * serialized across tabs through the Web Locks API.
 */

const CSRF_COOKIE_NAME = 'XSRF-TOKEN';
const CSRF_HEADER_NAME = 'X-XSRF-TOKEN';
const SESSION_HINT_KEY = 'authvolt.session.active';
const REFRESH_LOCK_NAME = 'authvolt-refresh';
const REQUEST_TIMEOUT_MS = 10000;

const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS']);

let accessToken = null;
let refreshPromise = null;
const sessionListeners = new Set();

const readCookie = (name) => {
  const prefix = `${name}=`;
  const entry = document.cookie.split('; ').find((part) => part.startsWith(prefix));
  return entry ? decodeURIComponent(entry.slice(prefix.length)) : null;
};

const timeoutSignal = () => AbortSignal.timeout?.(REQUEST_TIMEOUT_MS);

const csrfHeaders = () => {
  const csrfToken = readCookie(CSRF_COOKIE_NAME);
  return csrfToken ? { [CSRF_HEADER_NAME]: csrfToken } : {};
};

/**
 * Non-secret hint (never a credential) so anonymous visitors never trigger
 * a /refresh call on page load.
 */
export const hasSessionHint = () => {
  try {
    return localStorage.getItem(SESSION_HINT_KEY) === 'true';
  } catch {
    return false;
  }
};

export const setSessionHint = (active) => {
  try {
    if (active) {
      localStorage.setItem(SESSION_HINT_KEY, 'true');
    } else {
      localStorage.removeItem(SESSION_HINT_KEY);
    }
  } catch {
    // Storage unavailable (e.g. blocked); the session still works until reload.
  }
};

export const setAccessToken = (token) => {
  accessToken = token || null;
};

export const getTokenExpiryMs = (token) => {
  if (!token) return null;
  try {
    const payload = token.split('.')[1];
    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/');
    const padded = normalized + '='.repeat((4 - (normalized.length % 4)) % 4);
    const decoded = JSON.parse(atob(padded));
    return typeof decoded.exp === 'number' ? decoded.exp * 1000 : null;
  } catch {
    return null;
  }
};

const emitSessionEvent = (event) => {
  sessionListeners.forEach((listener) => {
    try {
      listener(event);
    } catch (error) {
      console.warn('[session] listener failed:', error);
    }
  });
};

export const registerSessionListener = (listener) => {
  sessionListeners.add(listener);
  return () => sessionListeners.delete(listener);
};

const clearSession = () => {
  accessToken = null;
  setSessionHint(false);
  emitSessionEvent({ type: 'expired' });
};

/**
 * The XSRF-TOKEN cookie is required by /refresh and /logout, and is seeded by
 * any response (CsrfCookieFilter). A browser restart can drop it while the
 * refresh cookie survives, so re-seed through the CSRF-exempt OPTIONS request
 * when it is missing.
 */
const ensureCsrfCookie = async () => {
  if (readCookie(CSRF_COOKIE_NAME)) return;
  try {
    await fetch('/api/v1/auth/login', {
      method: 'OPTIONS',
      credentials: 'include',
      signal: timeoutSignal(),
    });
  } catch {
    // Best effort — the follow-up request surfaces any real connectivity issue.
  }
};

const doRefresh = async () => {
  await ensureCsrfCookie();

  let response;
  try {
    response = await fetch('/api/v1/auth/refresh', {
      method: 'POST',
      credentials: 'include',
      // Content-Type is required by the nginx Lua validator for POSTs.
      headers: { 'Content-Type': 'application/json', ...csrfHeaders() },
      signal: timeoutSignal(),
    });
  } catch (error) {
    console.warn('[session] refresh request failed:', error);
    return null;
  }

  if (response.ok) {
    const data = await response.json().catch(() => null);
    if (!data?.accessToken) {
      clearSession();
      return null;
    }
    accessToken = data.accessToken;
    setSessionHint(true);
    emitSessionEvent({ type: 'refreshed', data });
    return data;
  }

  if ([400, 401, 403].includes(response.status)) {
    // Cookie missing, expired, revoked, or CSRF rejected: the session is gone.
    clearSession();
    return null;
  }

  // Everything else (429, 5xx, gateway) is transient — keep local state so a
  // later request or the rotation timer can retry.
  console.warn('[session] refresh failed transiently:', response.status);
  return null;
};

export const refreshSession = () => {
  if (!refreshPromise) {
    const attempt = globalThis.navigator?.locks?.request
      ? globalThis.navigator.locks.request(REFRESH_LOCK_NAME, doRefresh)
      : doRefresh();
    refreshPromise = attempt
      .catch((error) => {
        console.warn('[session] refresh failed:', error);
        return null;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
};

export const bootstrapSession = () => (hasSessionHint() ? refreshSession() : Promise.resolve(null));

export const logoutSession = async (bearerToken) => {
  try {
    await ensureCsrfCookie();
    // /logout is not on the gateway's public list, so the Lua validator
    // requires a Bearer-shaped header; Spring ignores it (permitAll). The
    // token is passed in because local state is cleared before this call.
    const headers = { 'Content-Type': 'application/json', ...csrfHeaders() };
    if (bearerToken) {
      headers['Authorization'] = `Bearer ${bearerToken}`;
    }
    const response = await fetch('/api/v1/auth/logout', {
      method: 'POST',
      credentials: 'include',
      headers,
      signal: timeoutSignal(),
    });
    if (!response.ok) {
      console.warn('[session] server logout returned:', response.status);
    }
  } catch (error) {
    console.warn('[session] server logout failed:', error);
  } finally {
    clearSession();
  }
};

export const getAuthHeaders = () => {
  const headers = { 'Content-Type': 'application/json' };
  if (accessToken) {
    headers['Authorization'] = `Bearer ${accessToken}`;
  }
  return headers;
};

export const authenticatedFetch = async (url, options = {}) => {
  const { _sessionRetried, ...fetchOptions } = options;
  const method = (fetchOptions.method || 'GET').toUpperCase();

  const headers = { ...getAuthHeaders() };
  if (!SAFE_METHODS.has(method)) {
    Object.assign(headers, csrfHeaders());
  }
  Object.assign(headers, fetchOptions.headers);

  const response = await fetch(url, {
    ...fetchOptions,
    method,
    headers,
    credentials: 'include', // send the refresh/CSRF cookies
    signal: fetchOptions.signal ?? timeoutSignal(),
  });

  if (response.status === 401 && !_sessionRetried && (accessToken || hasSessionHint())) {
    const refreshed = await refreshSession();
    if (refreshed) {
      return authenticatedFetch(url, { ...options, _sessionRetried: true });
    }
  }

  return response;
};

export const authGet = (url) => authenticatedFetch(url, { method: 'GET' });

export const authPost = (url, body) =>
  authenticatedFetch(url, { method: 'POST', body: JSON.stringify(body) });

export const authPut = (url, body = null) => {
  const options = { method: 'PUT' };
  if (body) {
    options.body = JSON.stringify(body);
  }
  return authenticatedFetch(url, options);
};

export const authDelete = (url) => authenticatedFetch(url, { method: 'DELETE' });
