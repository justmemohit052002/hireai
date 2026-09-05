// ─────────────────────────────────────────
// Messages API Stub
// TODO: Connect to FastAPI /messages endpoints
// ─────────────────────────────────────────
export const messagesApi = {
  // TODO: GET /conversations — list all conversations for current user
  getConversations: async () => {
    throw new Error('Not implemented — awaiting FastAPI integration');
  },

  // TODO: GET /conversations/:id/messages — paginated messages in a conversation
  getMessages: async (_conversationId, _page) => {
    throw new Error('Not implemented — awaiting FastAPI integration');
  },

  // TODO: POST /conversations/:id/messages — send a message
  sendMessage: async (_conversationId, _content) => {
    throw new Error('Not implemented — awaiting FastAPI integration');
  },

  // TODO: POST /conversations — start a new conversation
  createConversation: async (_participantId, _jobContextId) => {
    throw new Error('Not implemented — awaiting FastAPI integration');
  },

  // TODO: PATCH /conversations/:id/read — mark conversation as read
  markAsRead: async (_conversationId) => {
    throw new Error('Not implemented — awaiting FastAPI integration');
  },
};
