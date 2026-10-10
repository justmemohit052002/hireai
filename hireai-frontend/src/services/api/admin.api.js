import { apiClient } from './apiClient';

/**
 * Admin API Service
 * Interacts with backend administration endpoints for AI engine health,
 * LLM statistics, call log audit queries, and user role management.
 */
export const adminApi = {
  /**
   * Fetches AI Engine module health and reachability (cached TTL 30s on backend)
   */
  getHealth: () => apiClient.get('/admin/health'),

  /**
   * Fetches LLM usage, cost estimates, and quota tracking
   */
  getStats: () => apiClient.get('/admin/stats'),

  /**
   * Searchable AI Engine call logs with pagination and filters
   * @param {Object} params - { query, taskType, status, provider, page, size }
   */
  getLogs: (params = {}) => {
    const queryParams = new URLSearchParams();
    if (params.query) queryParams.set('query', params.query);
    if (params.taskType && params.taskType !== 'ALL') queryParams.set('taskType', params.taskType);
    if (params.status && params.status !== 'ALL') queryParams.set('status', params.status);
    if (params.provider && params.provider !== 'ALL') queryParams.set('provider', params.provider);
    if (params.page) queryParams.set('page', params.page);
    if (params.size) queryParams.set('size', params.size);

    const qs = queryParams.toString();
    return apiClient.get(`/admin/logs${qs ? `?${qs}` : ''}`);
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
};
