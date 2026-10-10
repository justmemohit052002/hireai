import { apiClient } from './apiClient';

export const notificationApi = {
  /**
   * Fetch latest notifications for the authenticated user
   */
  getNotifications: async (limit = 30) => {
    return apiClient.get(`/notifications?limit=${limit}`);
  },

  /**
   * Fetch current unread notification count
   */
  getUnreadCount: async () => {
    return apiClient.get('/notifications/unread-count');
  },

  /**
   * Mark a single notification as read
   */
  markAsRead: async (id) => {
    return apiClient.patch(`/notifications/${id}/read`);
  },

  /**
   * Mark all notifications as read
   */
  markAllAsRead: async () => {
    return apiClient.patch('/notifications/mark-all-read');
  },

  /**
   * Delete a single notification
   */
  deleteNotification: async (id) => {
    return apiClient.delete(`/notifications/${id}`);
  },

  /**
   * Clear all user notifications
   */
  clearAll: async () => {
    return apiClient.delete('/notifications/clear-all');
  },

  /**
   * Trigger a test notification (useful for verifying real-time delivery)
   */
  sendTestNotification: async (title, message) => {
    const params = new URLSearchParams();
    if (title) params.append('title', title);
    if (message) params.append('message', message);
    return apiClient.post(`/notifications/test?${params.toString()}`);
  },

  /**
   * Simulate a specific notification scenario (AI_MATCH, INTERVIEW_REMINDER, etc.)
   */
  simulateNotification: async (scenario = 'AI_MATCH') => {
    return apiClient.post(`/notifications/simulate?scenario=${encodeURIComponent(scenario)}`);
  },
};
