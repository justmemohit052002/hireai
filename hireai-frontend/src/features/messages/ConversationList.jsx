import React from 'react';
import { Avatar } from '@/components/ui/Avatar';
import { cn, formatRelativeTime } from '@/utils';

export const ConversationList = ({
  conversations,
  activeId,
  onSelect,
  currentUserId,
}) => {
  return (
    <div className="space-y-1 overflow-y-auto">
      {conversations.map((conv) => {
        const otherParticipant = conv.participantDetails.find((p) => p.id !== currentUserId) || conv.participantDetails[0];
        const isActive = conv.id === activeId;

        return (
          <button
            key={conv.id}
            onClick={() => onSelect(conv.id)}
            className={cn(
              'flex items-center gap-3 w-full p-3 rounded-2xl transition-all text-left border',
              isActive
                ? 'bg-brand-blue/10 border-blue-500/30 text-foreground shadow-sm'
                : 'border-transparent hover:bg-muted/60 text-muted-foreground'
            )}
          >
            <Avatar name={otherParticipant.name} size="md" />
            <div className="flex-1 min-w-0">
              <div className="flex items-center justify-between">
                <span className="font-bold text-xs text-foreground truncate">{otherParticipant.name}</span>
                <span className="text-[10px] text-muted-foreground shrink-0">
                  {formatRelativeTime(conv.updatedAt)}
                </span>
              </div>
              {conv.jobContext && (
                <span className="text-[10px] text-blue-500 font-semibold block truncate">
                  {conv.jobContext.title}
                </span>
              )}
              {conv.lastMessage && (
                <p className="text-xs text-muted-foreground truncate mt-0.5">
                  {conv.lastMessage.content}
                </p>
              )}
            </div>
            {conv.unreadCount > 0 && (
              <span className="w-2 h-2 rounded-full bg-blue-500 shrink-0" />
            )}
          </button>
        );
      })}
    </div>
  );
};
