import { apiClient } from './apiClient';

/**
 * Admin API Service
 * Interacts with backend administration endpoints for AI engine health,
 * LLM statistics, call log audit queries, and user role management.
 */
export const adminApi = {
  /**
   * Fetches AI Engine module health and reachability (cached snapshot polled every 45s on backend)
   */
  getHealth: () => apiClient.get('/admin/health'),

  /**
   * Fetches LLM usage, cost estimates, and quota tracking
   * @param {string} range - 'today' | 'week' | 'all'
   */
  getStats: (range = 'all') => {
    const qs = range ? `?range=${encodeURIComponent(range)}` : '';
    return apiClient.get(`/admin/stats${qs}`);
  },

  /**
   * Searchable AI Engine call logs with pagination and filters
   * @param {Object} params - { query, taskType, status, provider, from, to, correlationId, candidateRef, page, size }
   */
  getLogs: (params = {}) => {
    const queryParams = new URLSearchParams();
    if (params.query) queryParams.set('query', params.query);
    if (params.taskType && params.taskType !== 'ALL') queryParams.set('taskType', params.taskType);
    if (params.status && params.status !== 'ALL') queryParams.set('status', params.status);
    if (params.provider && params.provider !== 'ALL') queryParams.set('provider', params.provider);
    if (params.from) queryParams.set('from', params.from);
    if (params.to) queryParams.set('to', params.to);
    if (params.correlationId) queryParams.set('correlationId', params.correlationId);
    if (params.candidateRef) queryParams.set('candidateRef', params.candidateRef);
    if (params.page) queryParams.set('page', params.page);
    if (params.size) queryParams.set('size', params.size);

    const qs = queryParams.toString();
    return apiClient.get(`/admin/logs${qs ? `?${qs}` : ''}`);
  },

  /**
   * Fetches paginated administrative audit logs (newest first)
   * @param {Object} params - { page, size }
   */
  getAuditLogs: (params = {}) => {
    const queryParams = new URLSearchParams();
    if (params.page !== undefined) queryParams.set('page', params.page);
    if (params.size !== undefined) queryParams.set('size', params.size);

    const qs = queryParams.toString();
    return apiClient.get(`/admin/audit${qs ? `?${qs}` : ''}`);
  },

  /**
   * Fetches read-only provider configuration status and masked API keys
   */
  getProviders: () => apiClient.get('/admin/providers'),

  /**
   * Fetches system users list with search and role/status filtering
   * @param {Object} params - { search, role, enabled, page, size }
   */
  getUsers: (params = {}) => {
    const queryParams = new URLSearchParams();
    if (params.search) queryParams.set('search', params.search);
    if (params.role && params.role !== 'ALL') queryParams.set('role', params.role);
    if (params.enabled !== undefined && params.enabled !== null && params.enabled !== 'ALL') {
      queryParams.set('enabled', params.enabled);
    }
    if (params.page !== undefined) queryParams.set('page', params.page);
    if (params.size !== undefined) queryParams.set('size', params.size);

    const qs = queryParams.toString();
    return apiClient.get(`/admin/users${qs ? `?${qs}` : ''}`);
  },

  /**
   * Updates a user account's assigned role
   * @param {string} userId
   * @param {string} role - 'ROLE_ADMIN' | 'ROLE_RECRUITER' | 'ROLE_CANDIDATE'
   */
  updateUserRole: (userId, role) => apiClient.put(`/admin/users/${userId}/role`, { role }),

  /**
   * Soft-activates/deactivates or locks/unlocks a user account
   * @param {string} userId
   * @param {Object} statusData - { enabled: boolean, accountNonLocked?: boolean }
   */
  updateUserStatus: (userId, statusData) => apiClient.put(`/admin/users/${userId}/status`, statusData),

  /**
   * Comprehensive aggregate platform metrics (cached 1-5 mins)
   * @param {string} range - 'today' | '7d' | '30d'
   */
  getPlatformMetrics: (range = 'today') => apiClient.get(`/admin/platform-metrics?range=${encodeURIComponent(range)}`),

  /**
   * Creates a new sub-administrator account (ADMIN only)
   * @param {Object} payload - { firstName, lastName, email, password, confirmPassword }
   */
  createSubAdmin: (payload) => apiClient.post('/admin/sub-admins', payload),

  /**
   * Changes current user's password (used for forced first-login password change or self-service)
   * @param {Object} payload - { currentPassword, newPassword, confirmPassword }
   */
  changePassword: (payload) => apiClient.post('/admin/change-password', payload),

  /**
   * Test outbound email notification delivery
   */
  testEmail: (recipientEmail) => apiClient.post(`/admin/email/test?recipientEmail=${encodeURIComponent(recipientEmail)}`),
};
