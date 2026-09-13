/**
 * HireAI API Client
 * Centralized fetch client with automatic JWT header injection,
 * thread-safe single-flight token refresh handling, and standardized error extraction.
 */

const TOKEN_KEY = 'hireai_token';
const REFRESH_TOKEN_KEY = 'hireai_refresh_token';
const USER_KEY = 'hireai_user';

export const getStoredToken = () => localStorage.getItem(TOKEN_KEY) || '';
export const getStoredRefreshToken = () => localStorage.getItem(REFRESH_TOKEN_KEY) || '';
export const getStoredUser = () => {
  try {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
};

export const setAuthSession = (authData) => {
  if (!authData) return;
  if (authData.accessToken) {
    localStorage.setItem(TOKEN_KEY, authData.accessToken);
  }
  if (authData.refreshToken) {
    localStorage.setItem(REFRESH_TOKEN_KEY, authData.refreshToken);
  }
  const user = {
    id: authData.id || authData.userId,
    email: authData.email,
    firstName: authData.firstName,
    lastName: authData.lastName,
    phone: authData.phone,
    role: authData.role,
    companyName: authData.companyName,
  };
  localStorage.setItem(USER_KEY, JSON.stringify(user));
  localStorage.setItem('hireai_auth', JSON.stringify(authData));
};

export const clearAuthSession = () => {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
  localStorage.removeItem('hireai_auth');
  window.dispatchEvent(new Event('hireai-logout'));
};

let refreshPromise = null;

async function getRefreshedToken() {
  if (!refreshPromise) {
    refreshPromise = (async () => {
      const refreshToken = getStoredRefreshToken();
      if (!refreshToken) {
        clearAuthSession();
        throw new Error('Session expired. Please log in again.');
      }

      try {
        const response = await fetch('/auth/refresh', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ refreshToken }),
        });

        if (!response.ok) {
          clearAuthSession();
          throw new Error('Unable to refresh session');
        }

        const data = await response.json();
        const authData = data && typeof data === 'object' && 'data' in data ? data.data : data;
        setAuthSession(authData);
        return authData.accessToken;
      } catch (error) {
        clearAuthSession();
        throw error;
      }
    })().finally(() => {
      refreshPromise = null;
    });
  }
  return refreshPromise;
}

export async function request(endpoint, options = {}) {
  const {
    method = 'GET',
    headers = {},
    body,
    isFormData = false,
    requireAuth = true,
    ...restOptions
  } = options;

  const requestHeaders = new Headers(headers);

  if (!requestHeaders.has('Accept')) {
    requestHeaders.set('Accept', 'application/json, text/plain, */*');
  }

  if (!isFormData && !requestHeaders.has('Content-Type')) {
    requestHeaders.set('Content-Type', 'application/json');
  }

  const token = getStoredToken();
  if (requireAuth && token && !requestHeaders.has('Authorization')) {
    requestHeaders.set('Authorization', `Bearer ${token}`);
  }

  const config = {
    method,
    headers: requestHeaders,
    ...restOptions,
  };

  if (body) {
    config.body = isFormData ? body : typeof body === 'string' ? body : JSON.stringify(body);
  }

  try {
    let response = await fetch(endpoint, config);

    // Handle 401 Unauthorized - Attempt Token Refresh safely
    if (response.status === 401 && requireAuth && getStoredRefreshToken()) {
      try {
        const retryToken = await getRefreshedToken();
        requestHeaders.set('Authorization', `Bearer ${retryToken}`);
        response = await fetch(endpoint, {
          ...config,
          headers: requestHeaders,
        });
      } catch (refreshErr) {
        throw refreshErr;
      }
    }

    // Check for empty body / NoContent
    if (response.status === 204) {
      return null;
    }

    const contentType = response.headers.get('content-type') || '';
    let data;

    if (contentType.includes('application/json')) {
      data = await response.json().catch(() => ({}));
    } else if (contentType.includes('text/')) {
      data = await response.text();
    } else {
      data = await response.blob();
    }

    if (!response.ok) {
      const errorMsg =
        (data && (data.message || data.error || (data.errors && Object.values(data.errors).join(', ')))) ||
        `Request failed with status ${response.status}`;
      const err = new Error(errorMsg);
      err.status = response.status;
      err.data = data;
      throw err;
    }

    // Unwrap ApiResponse<T> format if backend wrapped it in { success: true, data: ..., message: ... }
    if (data && typeof data === 'object' && 'data' in data && 'success' in data) {
      return data.data;
    }

    return data;
  } catch (error) {
    console.error(`[API Error] ${method} ${endpoint}:`, error);
    throw error;
  }
}

export const apiClient = {
  get: (url, options = {}) => request(url, { ...options, method: 'GET' }),
  post: (url, body, options = {}) => request(url, { ...options, method: 'POST', body }),
  put: (url, body, options = {}) => request(url, { ...options, method: 'PUT', body }),
  patch: (url, body, options = {}) => request(url, { ...options, method: 'PATCH', body }),
  delete: (url, options = {}) => request(url, { ...options, method: 'DELETE' }),
  upload: (url, formData, options = {}) =>
    request(url, { ...options, method: 'POST', body: formData, isFormData: true }),
};
