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
    <div className="flex flex-col h-full space-y-3">
      {/* Search Filter */}
      <div className="relative">
        <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" />
        <input
          type="text"
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          placeholder="Search chats or roles..."
          className="w-full pl-9 pr-3 py-2 text-xs rounded-xl bg-muted/50 border border-border focus:outline-none focus:ring-2 focus:ring-blue-500/30 text-foreground"
        />
      </div>

      {/* Conversation Thread List */}
      <div className="space-y-1 overflow-y-auto flex-1 pr-1">
        {filteredConversations.length === 0 ? (
          <div className="text-center py-8 text-muted-foreground space-y-2">
            <MessageSquare className="w-8 h-8 mx-auto opacity-40" />
            <p className="text-xs">No conversations found</p>
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
                  'flex items-center gap-3 w-full p-3 rounded-2xl transition-all text-left border relative group',
                  isActive
                    ? 'bg-blue-600/10 border-blue-500/30 text-foreground shadow-sm'
                    : 'border-transparent hover:bg-muted/60 text-muted-foreground hover:text-foreground'
                )}
              >
                <div className="relative">
                  <Avatar name={partnerName} size="md" />
                  {conv.isOnline && (
                    <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 border-2 border-background absolute bottom-0 right-0" />
                  )}
                </div>

                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between">
                    <span className="font-bold text-xs text-foreground truncate">{partnerName}</span>
                    <span className="text-[10px] text-muted-foreground shrink-0 ml-1">
                      {formatRelativeTime(timestamp)}
                    </span>
                  </div>
                  {jobTitle && (
                    <span className="text-[10px] text-blue-500 font-semibold block truncate">
                      {jobTitle}
                    </span>
                  )}
                  <p className="text-xs text-muted-foreground truncate mt-0.5">
                    {lastText}
                  </p>
                </div>

                {unreadCount > 0 && (
                  <span className="px-1.5 py-0.5 rounded-full bg-blue-600 text-white text-[10px] font-bold shrink-0">
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
