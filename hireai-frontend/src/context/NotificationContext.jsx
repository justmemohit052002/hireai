import React, { createContext, useContext, useState, useEffect, useCallback, useRef } from 'react';
import { useAuth } from './AuthContext';
import { notificationApi } from '@/services/api/notification.api';

const NotificationContext = createContext(null);

// Audio chime using Web Audio API (smooth 2-tone melodic notification chime)
function playMelodicChime() {
  try {
    const AudioContextClass = window.AudioContext || window.webkitAudioContext;
    if (!AudioContextClass) return;
    const ctx = new AudioContextClass();
    if (ctx.state === 'suspended') {
      ctx.resume();
    }

    const now = ctx.currentTime;

    // First tone (E6 ~ 1318Hz)
    const osc1 = ctx.createOscillator();
    const gain1 = ctx.createGain();
    osc1.type = 'sine';
    osc1.frequency.setValueAtTime(1318.51, now);
    gain1.gain.setValueAtTime(0, now);
    gain1.gain.linearRampToValueAtTime(0.12, now + 0.03);
    gain1.gain.exponentialRampToValueAtTime(0.001, now + 0.28);
    osc1.connect(gain1);
    gain1.connect(ctx.destination);
    osc1.start(now);
    osc1.stop(now + 0.28);

    // Second harmonious tone (A6 ~ 1760Hz)
    const osc2 = ctx.createOscillator();
    const gain2 = ctx.createGain();
    osc2.type = 'sine';
    osc2.frequency.setValueAtTime(1760.00, now + 0.12);
    gain2.gain.setValueAtTime(0, now + 0.12);
    gain2.gain.linearRampToValueAtTime(0.15, now + 0.15);
    gain2.gain.exponentialRampToValueAtTime(0.001, now + 0.45);
    osc2.connect(gain2);
    gain2.connect(ctx.destination);
    osc2.start(now + 0.12);
    osc2.stop(now + 0.45);
  } catch {
    // Ignore audio autoplay restrictions
  }
}

export function NotificationProvider({ children }) {
  const { isAuthenticated } = useAuth();
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [isLoading, setIsLoading] = useState(false);
  const [isWiggling, setIsWiggling] = useState(false);
  const [toastNotification, setToastNotification] = useState(null);
  const toastTimeoutRef = useRef(null);

  // Fetch notifications and unread count from REST API
  const refreshNotifications = useCallback(async () => {
    if (!isAuthenticated) {
      setNotifications([]);
      setUnreadCount(0);
      return;
    }
    setIsLoading(true);
    try {
      const [listRes, countRes] = await Promise.all([
        notificationApi.getNotifications(30),
        notificationApi.getUnreadCount(),
      ]);

      const items = Array.isArray(listRes) ? listRes : listRes?.data || [];
      const count = countRes?.unreadCount ?? countRes?.data?.unreadCount ?? 0;

      setNotifications(items);
      setUnreadCount(Number(count));
    } catch (err) {
      console.warn('[NotificationContext] Failed to load notifications:', err.message);
    } finally {
      setIsLoading(false);
    }
  }, [isAuthenticated]);

  useEffect(() => {
    refreshNotifications();
  }, [refreshNotifications]);

  // Handle incoming real-time notification
  const handleRealtimeNotification = useCallback((incoming) => {
    if (!incoming || !incoming.id) return;

    // Deduplicate
    setNotifications((prev) => {
      if (prev.some((n) => n.id === incoming.id)) {
        return prev;
      }
      return [incoming, ...prev];
    });

    setUnreadCount((prev) => prev + 1);

    // Play subtle chime sound
    playMelodicChime();

    // Trigger visual wiggle animation on bell
    setIsWiggling(true);
    setTimeout(() => setIsWiggling(false), 1400);

    // Show temporary toast pop-up preview
    setToastNotification(incoming);
    if (toastTimeoutRef.current) clearTimeout(toastTimeoutRef.current);
    toastTimeoutRef.current = setTimeout(() => {
      setToastNotification(null);
    }, 4500);
  }, []);

  // Listen to the custom event dispatched by the WebSocket connection
  useEffect(() => {
    const onNotificationEvent = (event) => {
      if (event.detail) {
        handleRealtimeNotification(event.detail);
      }
    };

    window.addEventListener('hireai:new-notification', onNotificationEvent);
    return () => {
      window.removeEventListener('hireai:new-notification', onNotificationEvent);
      if (toastTimeoutRef.current) clearTimeout(toastTimeoutRef.current);
    };
  }, [handleRealtimeNotification]);

  // Mark single as read
  const markAsRead = useCallback(async (id) => {
    setNotifications((prev) =>
      prev.map((n) => (n.id === id ? { ...n, read: true } : n))
    );
    setUnreadCount((prev) => Math.max(0, prev - 1));

    try {
      await notificationApi.markAsRead(id);
    } catch (err) {
      console.warn('[NotificationContext] Failed to mark as read:', err.message);
    }
  }, []);

  // Mark all as read
  const markAllAsRead = useCallback(async () => {
    setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
    setUnreadCount(0);

    try {
      await notificationApi.markAllAsRead();
    } catch (err) {
      console.warn('[NotificationContext] Failed to mark all as read:', err.message);
    }
  }, []);

  // Delete single notification
  const deleteNotification = useCallback(async (id) => {
    const target = notifications.find((n) => n.id === id);
    setNotifications((prev) => prev.filter((n) => n.id !== id));
    if (target && !target.read) {
      setUnreadCount((prev) => Math.max(0, prev - 1));
    }

    try {
      await notificationApi.deleteNotification(id);
    } catch (err) {
      console.warn('[NotificationContext] Failed to delete notification:', err.message);
    }
  }, [notifications]);

  // Clear all notifications
  const clearAll = useCallback(async () => {
    setNotifications([]);
    setUnreadCount(0);

    try {
      await notificationApi.clearAll();
    } catch (err) {
      console.warn('[NotificationContext] Failed to clear all notifications:', err.message);
    }
  }, []);

  // Send a test notification
  const sendTestNotification = useCallback(async (title, message) => {
    try {
      const res = await notificationApi.sendTestNotification(title, message);
      const data = res?.data || res;
      if (data && data.id) {
        handleRealtimeNotification(data);
      }
    } catch (err) {
      console.warn('[NotificationContext] Failed to send test notification:', err.message);
    }
  }, [handleRealtimeNotification]);

  // Simulate specific scenario notification
  const simulateNotification = useCallback(async (scenario) => {
    try {
      const res = await notificationApi.simulateNotification(scenario);
      const data = res?.data || res;
      if (data && data.id) {
        handleRealtimeNotification(data);
      }
    } catch (err) {
      console.warn('[NotificationContext] Failed to simulate notification:', err.message);
    }
  }, [handleRealtimeNotification]);

  const dismissToast = useCallback(() => {
    setToastNotification(null);
  }, []);

  return (
    <NotificationContext.Provider
      value={{
        notifications,
        unreadCount,
        isLoading,
        isWiggling,
        toastNotification,
        refreshNotifications,
        markAsRead,
        markAllAsRead,
        deleteNotification,
        clearAll,
        sendTestNotification,
        simulateNotification,
        dismissToast,
        playChime: playMelodicChime,
      }}
    >
      {children}
    </NotificationContext.Provider>
  );
}

export function useNotification() {
  const context = useContext(NotificationContext);
  if (!context) {
    throw new Error('useNotification must be used within a NotificationProvider');
  }
  return context;
}
