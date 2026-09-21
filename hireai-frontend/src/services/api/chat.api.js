import { apiClient } from './apiClient';

export const chatApi = {
  /**
   * Fetch all conversations for the authenticated user
   */
  getConversations: async () => {
    return apiClient.get('/chat/conversations');
  },

  /**
   * Get specific conversation details
   */
  getConversationById: async (conversationId) => {
    return apiClient.get(`/chat/conversations/${conversationId}`);
  },

  /**
   * Create or fetch existing conversation
   */
  createConversation: async ({ candidateId, jobId, jobApplicationId, initialMessage }) => {
    return apiClient.post('/chat/conversations', {
      candidateId,
      jobId,
      jobApplicationId,
      initialMessage,
    });
  },

  /**
   * Fetch paginated messages for a conversation
   */
  getMessages: async (conversationId, page = 0, size = 30) => {
    return apiClient.get(`/chat/conversations/${conversationId}/messages?page=${page}&size=${size}`);
  },

  /**
   * Send message via REST API fallback
   */
  sendMessage: async (conversationId, messageData) => {
    return apiClient.post(`/chat/conversations/${conversationId}/messages`, messageData);
  },

  /**
   * Mark all unread messages in conversation as read
   */
  markAsRead: async (conversationId) => {
    return apiClient.patch(`/chat/conversations/${conversationId}/read`);
  },

  /**
   * Get total global unread count
   */
  getUnreadCount: async () => {
    return apiClient.get('/chat/unread-count');
  },

  /**
   * Upload file attachment for chat
   */
  uploadAttachment: async (conversationId, file) => {
    const formData = new FormData();
    formData.append('file', file);
    return apiClient.upload(`/chat/conversations/${conversationId}/attachments`, formData);
  },
};
