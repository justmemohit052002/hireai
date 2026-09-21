import React, { useState } from 'react';
import { Search, MessageSquare } from 'lucide-react';
import { Avatar } from '@/components/ui/Avatar';
import { cn, formatRelativeTime } from '@/utils';

export const ConversationList = ({
  conversations = [],
  activeId,
  onSelect,
  currentUserId,
}) => {
  const [searchQuery, setSearchQuery] = useState('');

  const filteredConversations = conversations.filter((conv) => {
    const partnerName = conv.partner
      ? `${conv.partner.firstName || ''} ${conv.partner.lastName || ''}`.trim()
      : conv.participantDetails?.find((p) => p.id !== currentUserId)?.name || '';
    const jobTitle = conv.jobTitle || conv.jobContext?.title || '';
    const query = searchQuery.toLowerCase();
    return partnerName.toLowerCase().includes(query) || jobTitle.toLowerCase().includes(query);
  });

  return (
    <div className="flex flex-col h-full min-h-0 space-y-2.5">
      {/* Search Filter */}
      <div className="relative shrink-0">
        <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" />
        <input
          type="text"
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          placeholder="Search chats or roles..."
          className="w-full pl-9 pr-3 py-2 text-xs rounded-xl bg-surface-2 border border-border focus:outline-none focus:border-brand-blue focus:ring-2 focus:ring-brand-blue/20 text-foreground placeholder:text-muted-foreground transition-all"
        />
      </div>

      {/* Conversation Thread List - strictly scrollable internally */}
      <div className="space-y-1.5 overflow-y-auto flex-1 min-h-0 pr-1">
        {filteredConversations.length === 0 ? (
          <div className="text-center py-10 text-muted-foreground space-y-2">
            <div className="w-10 h-10 rounded-2xl bg-brand-blue/15 text-brand-blue flex items-center justify-center mx-auto">
              <MessageSquare className="w-5 h-5 opacity-75" />
            </div>
            <p className="text-xs font-medium">No conversations found</p>
          </div>
        ) : (
          filteredConversations.map((conv) => {
            const partner = conv.partner || {};
            const partnerName = partner.firstName
              ? `${partner.firstName} ${partner.lastName || ''}`.trim()
              : conv.participantDetails?.find((p) => p.id !== currentUserId)?.name || 'User';

            const jobTitle = conv.jobTitle || conv.jobContext?.title;
            const lastText = conv.lastMessageText || conv.lastMessage?.content || 'Started conversation';
            const timestamp = conv.lastMessageAt || conv.updatedAt || conv.createdAt;
            const unreadCount = conv.unreadCount || 0;
            const isActive = conv.id === activeId;

            return (
              <button
                key={conv.id}
                onClick={() => onSelect(conv.id)}
                className={cn(
                  'flex items-center gap-3 w-full p-2.5 sm:p-3 rounded-2xl transition-all text-left border relative group cursor-pointer',
                  isActive
                    ? 'bg-brand-blue/15 dark:bg-brand-blue/20 border-brand-blue/40 text-foreground shadow-xs font-medium'
                    : 'border-transparent hover:bg-surface-2/60 text-muted-foreground hover:text-foreground'
                )}
              >
                {/* Active Left Indicator */}
                {isActive && (
                  <span className="absolute left-0 top-2.5 bottom-2.5 w-1 bg-brand-blue rounded-r-full" />
                )}

                <div className="relative shrink-0">
                  <Avatar name={partnerName} size="md" />
                  {conv.isOnline && (
                    <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 border-2 border-surface absolute bottom-0 right-0" />
                  )}
                </div>

                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between gap-1">
                    <span className="font-bold text-xs text-foreground truncate">{partnerName}</span>
                    <span className="text-[10px] text-muted-foreground shrink-0">
                      {formatRelativeTime(timestamp)}
                    </span>
                  </div>
                  {jobTitle && (
                    <span className="text-[10px] text-brand-blue font-semibold block truncate mt-0.5">
                      {jobTitle}
                    </span>
                  )}
                  <p className="text-xs text-muted-foreground truncate mt-0.5">
                    {lastText}
                  </p>
                </div>

                {unreadCount > 0 && (
                  <span className="px-2 py-0.5 rounded-full bg-brand-accent text-slate-950 text-[10px] font-black shadow-xs shrink-0">
                    {unreadCount}
                  </span>
                )}
              </button>
            );
          })
        )}
      </div>
    </div>
  );
};
