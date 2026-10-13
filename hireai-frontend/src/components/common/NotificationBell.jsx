import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Bell,
  Check,
  Sparkles,
  Briefcase,
  UserCheck,
  Calendar,
  MessageSquare,
  Info,
  Trash2,
  CheckCheck,
  Send,
  X,
  ExternalLink,
} from 'lucide-react';
import { Button } from '@/components/ui/Button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/Dropdown';
import { useNotification } from '@/context/NotificationContext';
import { cn } from '@/utils';

// Helper to format timestamps relatively
function formatRelativeTime(dateString) {
  if (!dateString) return 'Just now';
  try {
    const date = new Date(dateString);
    const now = new Date();
    const diffSec = Math.floor((now - date) / 1000);

    if (diffSec < 60) return 'Just now';
    const diffMin = Math.floor(diffSec / 60);
    if (diffMin < 60) return `${diffMin}m ago`;
    const diffHours = Math.floor(diffMin / 60);
    if (diffHours < 24) return `${diffHours}h ago`;
    const diffDays = Math.floor(diffHours / 24);
    if (diffDays < 7) return `${diffDays}d ago`;
    return date.toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
  } catch {
    return 'Recently';
  }
}

// Get icon and color badge by notification type
function getNotificationBadge(type) {
  switch (type) {
    case 'APPLICATION_STATUS':
      return {
        icon: <Briefcase className="w-3.5 h-3.5 text-emerald-400" />,
        bg: 'bg-emerald-500/10 border-emerald-500/20 text-emerald-400',
      };
    case 'NEW_APPLICATION':
      return {
        icon: <UserCheck className="w-3.5 h-3.5 text-indigo-400" />,
        bg: 'bg-indigo-500/10 border-indigo-500/20 text-indigo-400',
      };
    case 'AI_MATCH':
      return {
        icon: <Sparkles className="w-3.5 h-3.5 text-amber-400" />,
        bg: 'bg-amber-500/10 border-amber-500/20 text-amber-400',
      };
    case 'INTERVIEW':
      return {
        icon: <Calendar className="w-3.5 h-3.5 text-cyan-400" />,
        bg: 'bg-cyan-500/10 border-cyan-500/20 text-cyan-400',
      };
    case 'CHAT':
      return {
        icon: <MessageSquare className="w-3.5 h-3.5 text-sky-400" />,
        bg: 'bg-sky-500/10 border-sky-500/20 text-sky-400',
      };
    case 'SYSTEM':
    default:
      return {
        icon: <Info className="w-3.5 h-3.5 text-blue-400" />,
        bg: 'bg-blue-500/10 border-blue-500/20 text-blue-400',
      };
  }
}

export const NotificationBell = () => {
  const navigate = useNavigate();
  const {
    notifications,
    unreadCount,
    isLoading,
    isWiggling,
    toastNotification,
    markAsRead,
    markAllAsRead,
    deleteNotification,
    clearAll,
    sendTestNotification,
    simulateNotification,
    dismissToast,
  } = useNotification();

  const [activeTab, setActiveTab] = useState('all'); // 'all' | 'unread'
  const [isOpen, setIsOpen] = useState(false);
  const [isSendingTest, setIsSendingTest] = useState(false);

  const filteredNotifications =
    activeTab === 'unread'
      ? notifications.filter((n) => !n.read)
      : notifications;

  const handleNotificationClick = (item, e) => {
    e.stopPropagation();
    if (!item.read) {
      markAsRead(item.id);
    }
    if (item.link) {
      setIsOpen(false);
      let targetLink = item.link;
      if (targetLink === '/recruiter/candidates') {
        targetLink = '/recruiter/jobs';
      } else if (targetLink === '/notifications') {
        targetLink = '/recruiter/jobs';
      }
      navigate(targetLink);
    }
  };

  const handleTriggerTest = async (e) => {
    e.stopPropagation();
    setIsSendingTest(true);
    await sendTestNotification(
      'AI Match Alert',
      'Candidate scored 94% on Senior Full Stack Developer role!'
    );
    setTimeout(() => setIsSendingTest(false), 600);
  };

  return (
    <>
      {/* Floating Real-time Toast Pop-up when a notification arrives */}
      {toastNotification && (
        <aside
          role="status"
          aria-live="polite"
          aria-label="New notification preview"
          className="fixed top-20 right-4 sm:right-6 z-50 max-w-sm w-full p-4 rounded-2xl glass border border-blue-500/30 bg-surface-1/95 shadow-2xl backdrop-blur-xl animate-in slide-in-from-top-4 fade-in duration-300"
        >
          <div className="flex items-start gap-3">
            <div className="p-2 rounded-xl bg-blue-500/10 text-blue-400 shrink-0">
              <Sparkles className="w-4 h-4 animate-spin text-blue-400 duration-1000" />
            </div>
            <div
              className="flex-1 cursor-pointer pr-1"
              onClick={() => {
                dismissToast();
                if (toastNotification.link) {
                  let targetLink = toastNotification.link;
                  if (targetLink === '/recruiter/candidates') targetLink = '/recruiter/jobs';
                  else if (targetLink === '/notifications') targetLink = '/recruiter/jobs';
                  navigate(targetLink);
                }
              }}
            >
              <div className="flex items-center gap-1.5 font-bold text-xs text-foreground">
                <span className="w-2 h-2 rounded-full bg-blue-500 animate-ping inline-block" />
                <span>{toastNotification.title}</span>
              </div>
              <p className="text-xs text-muted-foreground mt-1 line-clamp-2 leading-relaxed">
                {toastNotification.message}
              </p>
              <span className="text-[10px] text-blue-400 font-medium mt-1.5 flex items-center gap-1 hover:underline">
                View details <ExternalLink className="w-2.5 h-2.5" />
              </span>
            </div>
            <button
              onClick={dismissToast}
              aria-label="Dismiss notification"
              className="p-1 rounded-lg text-muted-foreground hover:text-foreground hover:bg-surface-2 transition-colors cursor-pointer"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          </div>
        </aside>
      )}

      {/* Main Notification Bell Dropdown */}
      <DropdownMenu open={isOpen} onOpenChange={setIsOpen}>
        <DropdownMenuTrigger asChild>
          <Button
            variant="ghost"
            size="icon"
            className="relative rounded-full hover:bg-surface-2 transition-transform duration-200 active:scale-95 cursor-pointer"
            aria-label={`Notifications (${unreadCount} unread)`}
          >
            <Bell
              className={cn(
                'w-5 h-5 text-foreground transition-all duration-300',
                isWiggling && 'animate-wiggle text-blue-500 scale-110'
              )}
            />

            {/* Glowing Unread Badge */}
            {unreadCount > 0 && (
              <span className="absolute -top-0.5 -right-0.5 flex items-center justify-center min-w-4 h-4 px-1 rounded-full bg-gradient-to-r from-blue-500 to-indigo-600 text-[10px] font-extrabold text-white shadow-lg shadow-blue-500/30 ring-2 ring-background animate-pulse">
                {unreadCount > 9 ? '9+' : unreadCount}
              </span>
            )}
          </Button>
        </DropdownMenuTrigger>

        <DropdownMenuContent
          align="end"
          sideOffset={8}
          className="w-[90vw] sm:w-96 p-0 rounded-2xl glass border border-border/80 bg-surface-1/95 shadow-2xl backdrop-blur-2xl overflow-hidden animate-in fade-in zoom-in-95 duration-200 z-50"
        >
          {/* Header */}
          <div className="p-3.5 border-b border-border/50 bg-surface-2/40">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <div className="p-1.5 rounded-lg bg-blue-500/10 text-blue-400">
                  <Bell className="w-4 h-4" />
                </div>
                <div>
                  <h3 className="font-bold text-sm text-foreground flex items-center gap-2">
                    Notifications
                    {unreadCount > 0 && (
                      <span className="px-2 py-0.5 rounded-full text-[10px] font-semibold bg-blue-500/10 text-blue-400 border border-blue-500/20">
                        {unreadCount} new
                      </span>
                    )}
                  </h3>
                </div>
              </div>

              {unreadCount > 0 && (
                <button
                  onClick={markAllAsRead}
                  className="text-xs text-blue-400 hover:text-blue-300 flex items-center gap-1 font-medium transition-colors cursor-pointer px-2 py-1 rounded-lg hover:bg-blue-500/10"
                >
                  <CheckCheck className="w-3.5 h-3.5" /> Mark all read
                </button>
              )}
            </div>

            {/* Filter Tabs */}
            <div className="flex items-center gap-1 mt-3 p-1 rounded-xl bg-surface-3/50 border border-border/40 text-xs">
              <button
                type="button"
                onClick={() => setActiveTab('all')}
                className={cn(
                  'flex-1 py-1 px-3 rounded-lg font-semibold transition-all duration-150 cursor-pointer text-center',
                  activeTab === 'all'
                    ? 'bg-blue-500 text-white shadow-sm font-bold'
                    : 'text-muted-foreground hover:text-foreground'
                )}
              >
                All ({notifications.length})
              </button>
              <button
                type="button"
                onClick={() => setActiveTab('unread')}
                className={cn(
                  'flex-1 py-1 px-3 rounded-lg font-semibold transition-all duration-150 cursor-pointer text-center',
                  activeTab === 'unread'
                    ? 'bg-blue-500 text-white shadow-sm font-bold'
                    : 'text-muted-foreground hover:text-foreground'
                )}
              >
                Unread ({unreadCount})
              </button>
            </div>
          </div>

          {/* Notifications List */}
          <div className="max-h-80 overflow-y-auto divide-y divide-border/30 scrollbar-thin">
            {isLoading && notifications.length === 0 ? (
              <div className="flex items-center justify-center p-8 text-xs text-muted-foreground gap-2">
                <div className="w-4 h-4 border-2 border-blue-500 border-t-transparent rounded-full animate-spin" />
                Loading notifications...
              </div>
            ) : filteredNotifications.length === 0 ? (
              <div className="flex flex-col items-center justify-center p-8 text-center">
                <div className="p-3 rounded-full bg-surface-2 text-muted-foreground mb-2">
                  <Check className="w-5 h-5 text-emerald-400" />
                </div>
                <p className="font-semibold text-xs text-foreground">You're all caught up!</p>
                <p className="text-[11px] text-muted-foreground mt-0.5">
                  {activeTab === 'unread'
                    ? 'No unread notifications at the moment.'
                    : 'No new notifications to display.'}
                </p>
              </div>
            ) : (
              filteredNotifications.map((n) => {
                const badge = getNotificationBadge(n.type);
                return (
                  <DropdownMenuItem
                    key={n.id}
                    onSelect={(e) => e.preventDefault()}
                    onClick={(e) => handleNotificationClick(n, e)}
                    className={cn(
                      'group relative flex items-start gap-3 p-3.5 cursor-pointer transition-colors duration-150 focus:bg-surface-2/80 hover:bg-surface-2/60',
                      !n.read && 'bg-blue-500/5'
                    )}
                  >
                    {/* Category Icon Badge */}
                    <div
                      className={cn(
                        'p-2 rounded-xl border shrink-0 mt-0.5 transition-transform group-hover:scale-105',
                        badge.bg
                      )}
                    >
                      {badge.icon}
                    </div>

                    {/* Content */}
                    <div className="flex-1 min-w-0 pr-6">
                      <div className="flex items-center justify-between gap-1 mb-0.5">
                        <span className="font-semibold text-xs text-foreground truncate flex items-center gap-1.5">
                          {!n.read && (
                            <span className="w-1.5 h-1.5 rounded-full bg-blue-500 shrink-0" />
                          )}
                          {n.title}
                        </span>
                        <span className="text-[10px] text-muted-foreground shrink-0">
                          {formatRelativeTime(n.createdAt)}
                        </span>
                      </div>
                      <p className="text-xs text-muted-foreground leading-relaxed line-clamp-2">
                        {n.message}
                      </p>
                      {n.link && (
                        <span className="inline-flex items-center gap-1 text-[10px] text-blue-400 font-medium mt-1 group-hover:underline">
                          Open <ExternalLink className="w-2.5 h-2.5" />
                        </span>
                      )}
                    </div>

                    {/* Action buttons on hover */}
                    <div className="absolute right-2 top-3 flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                      <button
                        title="Delete notification"
                        onClick={(e) => {
                          e.stopPropagation();
                          deleteNotification(n.id);
                        }}
                        className="p-1 rounded-md text-muted-foreground hover:text-rose-400 hover:bg-rose-500/10 transition-colors"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </DropdownMenuItem>
                );
              })
            )}
          </div>

          {/* Footer Simulation Scenarios & Actions */}
          <div className="p-2.5 border-t border-border/50 bg-surface-2/30 text-xs flex flex-col gap-2">
            <div className="flex items-center justify-between">
              <span className="text-[10px] font-bold text-muted-foreground uppercase tracking-wider">
                Simulate Live Event
              </span>
              {notifications.length > 0 && (
                <button
                  onClick={clearAll}
                  className="text-[11px] text-muted-foreground hover:text-rose-400 font-medium transition-colors px-2 py-0.5 rounded-lg hover:bg-surface-3 cursor-pointer"
                >
                  Clear all
                </button>
              )}
            </div>

            {/* Quick Scenario Buttons */}
            <div className="flex flex-wrap items-center gap-1.5">
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  simulateNotification('AI_MATCH');
                }}
                className="text-[10px] font-semibold px-2 py-1 rounded-lg bg-amber-500/10 text-amber-500 border border-amber-500/20 hover:bg-amber-500/20 transition-colors cursor-pointer"
              >
                AI Shortlist (88%)
              </button>
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  simulateNotification('INTERVIEW_REMINDER');
                }}
                className="text-[10px] font-semibold px-2 py-1 rounded-lg bg-cyan-500/10 text-cyan-600 dark:text-cyan-400 border border-cyan-500/20 hover:bg-cyan-500/20 transition-colors cursor-pointer"
              >
                Interview in 15m
              </button>
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  simulateNotification('STATUS_UPDATE');
                }}
                className="text-[10px] font-semibold px-2 py-1 rounded-lg bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20 hover:bg-emerald-500/20 transition-colors cursor-pointer"
              >
                Offer Released
              </button>
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  simulateNotification('SECURITY_ALERT');
                }}
                className="text-[10px] font-semibold px-2 py-1 rounded-lg bg-rose-500/10 text-rose-500 border border-rose-500/20 hover:bg-rose-500/20 transition-colors cursor-pointer"
              >
                Security Alert
              </button>
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  simulateNotification('RESUME_FEEDBACK');
                }}
                className="text-[10px] font-semibold px-2 py-1 rounded-lg bg-brand-blue/10 text-brand-blue border border-brand-blue/20 hover:bg-brand-blue/20 transition-colors cursor-pointer"
              >
                Resume ATS Score
              </button>
            </div>
          </div>
        </DropdownMenuContent>
      </DropdownMenu>
    </>
  );
};
