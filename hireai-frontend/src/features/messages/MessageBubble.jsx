import React from 'react';
import { cn, formatRelativeTime } from '@/utils';

export const MessageBubble = ({ message, isSelf }) => {
  return (
    <div className={cn('flex flex-col mb-3 max-w-[80%]', isSelf ? 'ml-auto items-end' : 'mr-auto items-start')}>
      <div
        className={cn(
          'p-3.5 rounded-2xl text-sm leading-relaxed shadow-sm',
          isSelf
            ? 'bg-blue-600 text-white rounded-br-none font-medium'
            : 'surface-nested border border-white/20 dark:border-white/10 text-foreground rounded-bl-none'
        )}
      >
        {message.content}
      </div>
      <span className="text-[10px] text-muted-foreground mt-1 px-1">
        {formatRelativeTime(message.sentAt)}
      </span>
    </div>
  );
};
