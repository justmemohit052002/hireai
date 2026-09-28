import { apiClient, setAuthSession, clearAuthSession } from './apiClient';

export const authApi = {
  /** Authenticates candidate or recruiter and returns JWT tokens */
  login: async (email, password, role) => {
    const payload = { email, password };
    if (role) {
      payload.role = typeof role === 'string' ? role.toUpperCase() : role;
    }
    const data = await apiClient.post('/auth/login', payload, { requireAuth: false });
    setAuthSession(data);
    return data;
  },

  /** Registers a new candidate (sends 6-digit verification OTP) */
  registerCandidate: async ({ firstName, lastName, email, phoneNumber, password }) => {
    const data = await apiClient.post(
      '/auth/register/candidate',
      { firstName, lastName, email, phoneNumber, password },
      { requireAuth: false }
    );
    if (data && data.accessToken) {
      setAuthSession(data);
    }
    return data;
  },

  /** Registers a new recruiter (sends 6-digit verification OTP) */
  registerRecruiter: async ({ firstName, lastName, email, phoneNumber, password, companyName }) => {
    const data = await apiClient.post(
      '/auth/register/recruiter',
      { firstName, lastName, email, phoneNumber, password, companyName },
      { requireAuth: false }
    );
    if (data && data.accessToken) {
      setAuthSession(data);
    }
    return data;
  },

  /** Verifies 6-digit email OTP and activates user account, issuing JWT tokens */
  verifyEmailOtp: async ({ email, otp }) => {
    const data = await apiClient.post(
      '/auth/verify-email-otp',
      { email, otp },
      { requireAuth: false }
    );
    if (data && data.accessToken) {
      setAuthSession(data);
    }
    return data;
  },

  /** Resends 6-digit email verification OTP with 60s cooldown */
  resendEmailOtp: async ({ email }) => {
    return apiClient.post(
      '/auth/resend-email-otp',
      { email },
      { requireAuth: false }
    );
  },

  /** Authenticates or registers using Google OAuth */
  loginWithGoogle: async (payload) => {
    const data = await apiClient.post(
      '/auth/oauth/google',
      payload,
      { requireAuth: false }
    );
    if (data && data.accessToken) {
      setAuthSession(data);
    }
    return data;
  },

  /** Authenticates or registers using LinkedIn OAuth */
  loginWithLinkedIn: async (payload) => {
    const data = await apiClient.post(
      '/auth/oauth/linkedin',
      payload,
      { requireAuth: false }
    );
    if (data && data.accessToken) {
      setAuthSession(data);
    }
    return data;
  },

  /** Refreshes JWT tokens */
  refreshToken: async (refreshToken) => {
    const data = await apiClient.post('/auth/refresh', { refreshToken }, { requireAuth: false });
    setAuthSession(data);
    return data;
  },

  /** Logs user out server-side and clears local storage */
  logout: async () => {
    const refreshToken = localStorage.getItem('hireai_refresh_token');
    try {
      if (refreshToken) {
        await apiClient.post('/auth/logout', { refreshToken });
      }
    } catch (e) {
      console.warn('Logout notification failed:', e);
    } finally {
      clearAuthSession();
    }
  },

  /** Initiates forgot password email flow */
  forgotPassword: async (email) => {
    return apiClient.post('/auth/forgot-password', { email }, { requireAuth: false });
  },

  /** Verifies reset token */
  verifyResetToken: async (token) => {
    return apiClient.get(`/auth/verify-reset-token?token=${encodeURIComponent(token)}`, {
      requireAuth: false,
    });
  },

  /** Resets password using token */
  resetPassword: async (token, newPassword) => {
    return apiClient.post('/auth/reset-password', { token, newPassword }, { requireAuth: false });
  },

  /** Fetches current authenticated user profile */
  getMe: async () => {
    return apiClient.get('/users/me');
  },

  /** Updates user profile */
  updateMe: async (userData) => {
    return apiClient.put('/users/me', userData);
  },
};
