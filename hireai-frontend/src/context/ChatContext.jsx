import React, { createContext, useContext, useState, useEffect, useCallback, useRef } from 'react';
import { useAuth } from './AuthContext';
import { chatApi } from '@/services/api/chat.api';
import { useChatSocket } from '@/hooks/useChatSocket';

const ChatContext = createContext(null);

export function ChatProvider({ children }) {
  const { user, isAuthenticated } = useAuth();
  const [conversations, setConversations] = useState([]);
  const [activeConversationId, setActiveConversationId] = useState(null);
  const [messagesMap, setMessagesMap] = useState({}); // { [convId]: [ ...messages ] }
  const [unreadTotal, setUnreadTotal] = useState(0);
  const [isLoadingConversations, setIsLoadingConversations] = useState(false);
  const [isLoadingMessages, setIsLoadingMessages] = useState(false);
  const [typingState, setTypingState] = useState({}); // { [convId]: boolean }

  const activeIdRef = useRef(activeConversationId);
  activeIdRef.current = activeConversationId;

  // Fetch conversations list
  const refreshConversations = useCallback(async () => {
    if (!isAuthenticated) return;
    setIsLoadingConversations(true);
    try {
      const convList = await chatApi.getConversations();
      setConversations(convList || []);
      const unreadData = await chatApi.getUnreadCount();
      setUnreadTotal(unreadData?.totalUnread || 0);
    } catch (err) {
      console.warn('[ChatContext] Failed to load conversations', err.message);
    } finally {
      setIsLoadingConversations(false);
    }
  }, [isAuthenticated]);

  // Real-time message receiver
  const handleMessageReceived = useCallback((msg) => {
    if (!msg || !msg.conversationId) return;

    setMessagesMap((prev) => {
      const existing = prev[msg.conversationId] || [];
      // Deduplicate if real ID already in list
      if (existing.some((m) => m.id === msg.id)) {
        return prev;
      }
      // Replace optimistic matching message or append
      let replaced = false;
      const updated = existing.map((m) => {
        if (!replaced && m.isOptimistic && m.senderId === msg.senderId && (m.messageText === msg.messageText || m.attachmentName === msg.attachmentName)) {
          replaced = true;
          return msg;
        }
        return m;
      });

      return {
        ...prev,
        [msg.conversationId]: replaced ? updated : [...updated, msg],
      };
    });

    // Update conversation list preview & unread badge
    setConversations((prev) => {
      let found = false;
      const nextList = prev.map((c) => {
        if (c.id === msg.conversationId) {
          found = true;
          const isCurrentlyActive = activeIdRef.current === c.id;
          const newUnread = isCurrentlyActive ? 0 : (c.unreadCount || 0) + 1;
          return {
            ...c,
            lastMessageText: msg.messageText || (msg.attachmentName ? `📎 ${msg.attachmentName}` : 'New message'),
            lastMessageAt: msg.createdAt || new Date().toISOString(),
            unreadCount: newUnread,
          };
        }
        return c;
      });

      // If conversation is brand new, refresh list
      if (!found) {
        refreshConversations();
      }
      return nextList;
    });

    // If message is in active chat, immediately mark as read
    if (activeIdRef.current === msg.conversationId) {
      chatApi.markAsRead(msg.conversationId).catch(() => {});
    } else {
      setUnreadTotal((prev) => prev + 1);
    }
  }, [refreshConversations]);

  // Real-time typing receiver
  const handleTypingReceived = useCallback((typingData) => {
    if (!typingData || !typingData.conversationId) return;
    setTypingState((prev) => ({
      ...prev,
      [typingData.conversationId]: typingData.isTyping,
    }));
  }, []);

  // Real-time read receipt receiver
  const handleReadReceiptReceived = useCallback((convId) => {
    if (!convId) return;
    setMessagesMap((prev) => {
      const msgs = prev[convId];
      if (!msgs) return prev;
      return {
        ...prev,
        [convId]: msgs.map((m) => ({ ...m, status: 'READ' })),
      };
    });
  }, []);

  // Initialize WebSocket connection
  const { isConnected, sendMessage: socketSendMessage, sendTyping: socketSendTyping, sendMarkRead } = useChatSocket({
    onMessageReceived: handleMessageReceived,
    onTypingReceived: handleTypingReceived,
    onReadReceiptReceived: handleReadReceiptReceived,
  });

  // Load message history for a conversation
  const loadMessages = useCallback(async (convId) => {
    if (!convId) return;
    setIsLoadingMessages(true);
    try {
      const paged = await chatApi.getMessages(convId, 0, 50);
      const list = paged?.content ? [...paged.content].reverse() : [];
      setMessagesMap((prev) => ({
        ...prev,
        [convId]: list,
      }));

      // Mark as read in backend
      await chatApi.markAsRead(convId);
      sendMarkRead(convId);

      // Decrement unread counter in state
      setConversations((prev) =>
        prev.map((c) => (c.id === convId ? { ...c, unreadCount: 0 } : c))
      );
    } catch (err) {
      console.error('[ChatContext] Failed to load messages', err);
    } finally {
      setIsLoadingMessages(false);
    }
  }, [sendMarkRead]);

  // Select active conversation
  const selectConversation = useCallback((convId) => {
    setActiveConversationId(convId);
    if (convId) {
      loadMessages(convId);
    }
  }, [loadMessages]);

  // Send a message (Optimistic + WebSocket / REST fallback)
  const sendMessage = useCallback(
    async (text, { messageType = 'TEXT', attachmentUrl = null, attachmentName = null, attachmentSize = null, metadataJson = null } = {}) => {
      if (!activeConversationId || !user) return;

      const activeConv = conversations.find((c) => c.id === activeConversationId);
      const recipientId = activeConv?.partner?.id;

      if (!recipientId) return;

      // 1. Optimistic Message Entry
      const optimisticMsg = {
        id: `opt-${Date.now()}`,
        conversationId: activeConversationId,
        senderId: user.id,
        senderName: `${user.firstName || ''} ${user.lastName || ''}`.trim() || 'You',
        recipientId,
        messageText: text,
        messageType,
        attachmentUrl,
        attachmentName,
        attachmentSize,
        status: 'SENT',
        createdAt: new Date().toISOString(),
        isOptimistic: true,
      };

      setMessagesMap((prev) => ({
        ...prev,
        [activeConversationId]: [...(prev[activeConversationId] || []), optimisticMsg],
      }));

      // 2. Dispatch via WebSocket or REST fallback
      const payload = {
        conversationId: activeConversationId,
        recipientId,
        messageText: text,
        messageType,
        attachmentUrl,
        attachmentName,
        attachmentSize,
        metadataJson,
      };

      const sentViaSocket = socketSendMessage(payload);
      if (!sentViaSocket) {
        try {
          const savedMsg = await chatApi.sendMessage(activeConversationId, payload);
          setMessagesMap((prev) => ({
            ...prev,
            [activeConversationId]: (prev[activeConversationId] || []).map((m) =>
              m.id === optimisticMsg.id ? savedMsg : m
            ),
          }));
        } catch (err) {
          console.error('[ChatContext] Failed to send message via REST', err);
        }
      }
    },
    [activeConversationId, user, conversations, socketSendMessage]
  );

  // Send typing indicator
  const sendTyping = useCallback(
    (isTyping) => {
      if (!activeConversationId) return;
      const activeConv = conversations.find((c) => c.id === activeConversationId);
      if (activeConv?.partner?.id) {
        socketSendTyping({
          conversationId: activeConversationId,
          recipientId: activeConv.partner.id,
          isTyping,
        });
      }
    },
    [activeConversationId, conversations, socketSendTyping]
  );

  // Start or open a conversation with candidate
  const startConversation = useCallback(
    async ({ candidateId, jobId, jobApplicationId, initialMessage }) => {
      try {
        const conv = await chatApi.createConversation({
          candidateId,
          jobId,
          jobApplicationId,
          initialMessage,
        });

        await refreshConversations();
        if (conv?.id) {
          selectConversation(conv.id);
        }
        return conv;
      } catch (err) {
        console.error('[ChatContext] Failed to create conversation', err);
        throw err;
      }
    },
    [refreshConversations, selectConversation]
  );

  useEffect(() => {
    if (isAuthenticated) {
      refreshConversations();
    } else {
      setConversations([]);
      setMessagesMap({});
      setActiveConversationId(null);
      setUnreadTotal(0);
    }
  }, [isAuthenticated, refreshConversations]);

  const activeConversation = conversations.find((c) => c.id === activeConversationId) || null;
  const activeMessages = activeConversationId ? messagesMap[activeConversationId] || [] : [];
  const isPartnerTyping = activeConversationId ? Boolean(typingState[activeConversationId]) : false;

  return (
    <ChatContext.Provider
      value={{
        conversations,
        activeConversationId,
        activeConversation,
        activeMessages,
        unreadTotal,
        isConnected,
        isPartnerTyping,
        isLoadingConversations,
        isLoadingMessages,
        selectConversation,
        sendMessage,
        sendTyping,
        startConversation,
        refreshConversations,
      }}
    >
      {children}
    </ChatContext.Provider>
  );
}

export function useChat() {
  const context = useContext(ChatContext);
  if (!context) {
    throw new Error('useChat must be used within a ChatProvider');
  }
  return context;
}
