import React, { useState } from 'react';
import { Bell, Check, Sparkles } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/Dropdown';

const MOCK_NOTIFICATIONS = [
  {
    id: 'n1',
    title: 'AI Resume Match',
    message: 'Your profile is a 98% match for Senior Frontend at Anthropic',
    time: '10m ago',
    read: false,
  },
  {
    id: 'n2',
    title: 'New Message',
    message: 'Jordan Kim sent you a message regarding your application',
    time: '1h ago',
    read: false,
  },
  {
    id: 'n3',
    title: 'Application Status Update',
    message: 'Linear updated your application to Interview stage',
    time: '1d ago',
    read: true,
  },
];

export const NotificationBell = () => {
  const [notifications, setNotifications] = useState(MOCK_NOTIFICATIONS);
  const unreadCount = notifications.filter((n) => !n.read).length;

  const markAllAsRead = () => {
    setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
  };

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant="ghost" size="icon" className="relative rounded-full">
          <Bell className="w-5 h-5 text-foreground" />
          {unreadCount > 0 && (
            <span className="absolute top-1 right-1 flex h-4 w-4 items-center justify-center rounded-full bg-blue-600 text-[10px] font-bold text-white ring-2 ring-background">
              {unreadCount}
            </span>
          )}
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-80 p-0">
        <div className="flex items-center justify-between p-3.5 border-b border-border/50">
          <div className="flex items-center gap-1.5 font-semibold text-sm">
            <Sparkles className="w-4 h-4 text-blue-500" />
            <span>Notifications</span>
          </div>
          {unreadCount > 0 && (
            <button
              onClick={markAllAsRead}
              className="text-xs text-blue-500 hover:underline flex items-center gap-1 font-medium"
            >
              <Check className="w-3 h-3" /> Mark all read
            </button>
          )}
        </div>
        <div className="max-h-72 overflow-y-auto divide-y divide-border/40">
          {notifications.map((n) => (
            <DropdownMenuItem
              key={n.id}
              className="flex flex-col items-start gap-1 p-3 cursor-pointer focus:bg-muted/80"
            >
              <div className="flex items-center justify-between w-full">
                <span className="font-semibold text-xs text-foreground flex items-center gap-1.5">
                  {!n.read && <span className="w-1.5 h-1.5 rounded-full bg-blue-500" />}
                  {n.title}
                </span>
                <span className="text-[10px] text-muted-foreground">{n.time}</span>
              </div>
              <p className="text-xs text-muted-foreground leading-relaxed">{n.message}</p>
            </DropdownMenuItem>
          ))}
        </div>
      </DropdownMenuContent>
    </DropdownMenu>
  );
};
