import { useEffect, useRef, useState, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import { getStoredToken } from '@/services/api/apiClient';

/**
 * Custom React Hook for STOMP WebSocket communication using native WebSockets
 */
export function useChatSocket({ onMessageReceived, onTypingReceived, onReadReceiptReceived } = {}) {
  const [isConnected, setIsConnected] = useState(false);
  const clientRef = useRef(null);

  // Keep callback refs fresh without causing reconnection cycles
  const onMessageRef = useRef(onMessageReceived);
  onMessageRef.current = onMessageReceived;

  const onTypingRef = useRef(onTypingReceived);
  onTypingRef.current = onTypingReceived;

  const onReadReceiptRef = useRef(onReadReceiptReceived);
  onReadReceiptRef.current = onReadReceiptReceived;

  useEffect(() => {
    const token = getStoredToken();
    if (!token) {
      setIsConnected(false);
      return;
    }

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const brokerURL = `${protocol}//${window.location.host}/ws/chat`;

    const client = new Client({
      brokerURL,
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      debug: () => {},
      onConnect: () => {
        setIsConnected(true);

        // 1. Subscribe to incoming private messages
        client.subscribe('/user/queue/messages', (messageFrame) => {
          try {
            const parsed = JSON.parse(messageFrame.body);
            if (onMessageRef.current) {
              onMessageRef.current(parsed);
            }
          } catch (err) {
            console.error('[STOMP] Failed to parse message body', err);
          }
        });

        // 2. Subscribe to typing events
        client.subscribe('/user/queue/typing', (messageFrame) => {
          try {
            const parsed = JSON.parse(messageFrame.body);
            if (onTypingRef.current) {
              onTypingRef.current(parsed);
            }
          } catch (err) {
            console.error('[STOMP] Failed to parse typing body', err);
          }
        });

        // 3. Subscribe to read receipts
        client.subscribe('/user/queue/read-receipt', (messageFrame) => {
          try {
            const convId = messageFrame.body;
            if (onReadReceiptRef.current) {
              onReadReceiptRef.current(convId);
            }
          } catch (err) {
            console.error('[STOMP] Failed to parse read receipt', err);
          }
        });
      },
      onDisconnect: () => {
        setIsConnected(false);
      },
      onStompError: (frame) => {
        console.warn('[STOMP Error]', frame?.headers?.['message'], frame?.body);
        setIsConnected(false);
      },
      onWebSocketError: (event) => {
        console.warn('[WebSocket Error]', event);
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      if (client.active) {
        client.deactivate();
      }
      setIsConnected(false);
    };
  }, []);

  const sendMessage = useCallback((payload) => {
    if (clientRef.current && clientRef.current.connected) {
      clientRef.current.publish({
        destination: '/app/chat.send',
        body: JSON.stringify(payload),
      });
      return true;
    }
    return false;
  }, []);

  const sendTyping = useCallback((payload) => {
    if (clientRef.current && clientRef.current.connected) {
      clientRef.current.publish({
        destination: '/app/chat.typing',
        body: JSON.stringify(payload),
      });
    }
  }, []);

  const sendMarkRead = useCallback((conversationId) => {
    if (clientRef.current && clientRef.current.connected) {
      clientRef.current.publish({
        destination: '/app/chat.read',
        body: JSON.stringify(conversationId),
      });
    }
  }, []);

  return {
    isConnected,
    sendMessage,
    sendTyping,
    sendMarkRead,
  };
}
