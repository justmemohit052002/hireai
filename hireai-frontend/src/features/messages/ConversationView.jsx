import React, { useState } from 'react';
import { Send, Paperclip, Sparkles, Building2 } from 'lucide-react';
import { Avatar } from '@/components/ui/Avatar';
import { Button } from '@/components/ui/Button';
import { MessageBubble } from './MessageBubble';

export const ConversationView = ({
  conversation,
  messages,
  currentUserId,
  onSendMessage,
}) => {
  const [text, setText] = useState('');
  const otherParticipant = conversation.participantDetails.find((p) => p.id !== currentUserId) || conversation.participantDetails[0];

  const handleSend = (e) => {
    e.preventDefault();
    if (!text.trim()) return;
    onSendMessage(text);
    setText('');
  };

  return (
    <div className="flex flex-col h-full glass rounded-[24px] border border-white/20 dark:border-white/10 overflow-hidden shadow-2xl">
      {/* Header */}
      <div className="flex items-center justify-between p-4 border-b border-border/50 bg-background/40">
        <div className="flex items-center gap-3">
          <Avatar name={otherParticipant.name} size="md" />
          <div>
            <h3 className="font-bold text-sm font-heading text-foreground">{otherParticipant.name}</h3>
            {conversation.jobContext && (
              <p className="text-xs text-blue-500 font-medium flex items-center gap-1">
                <Building2 className="w-3 h-3" /> Re: {conversation.jobContext.title}
              </p>
            )}
          </div>
        </div>

        <div className="flex items-center gap-1.5 px-3 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/20 text-xs font-bold text-emerald-500">
          <Sparkles className="w-3.5 h-3.5" />
          <span>FastAPI Chat Active</span>
        </div>
      </div>

      {/* Message Feed */}
      <div className="flex-1 overflow-y-auto p-4 space-y-3">
        {messages.map((msg) => (
          <MessageBubble key={msg.id} message={msg} isSelf={msg.senderId === currentUserId} />
        ))}
      </div>

      {/* Input Bar */}
      <form onSubmit={handleSend} className="p-3 border-t border-border/50 bg-background/50 flex items-center gap-2">
        <button
          type="button"
          className="p-2 rounded-xl text-muted-foreground hover:text-foreground hover:bg-muted transition-colors"
        >
          <Paperclip className="w-4 h-4" />
        </button>

        <input
          type="text"
          value={text}
          onChange={(e) => setText(e.target.value)}
          placeholder="Type your message..."
          className="flex-1 bg-transparent text-sm text-foreground placeholder:text-muted-foreground focus:outline-none px-2"
        />

        <Button type="submit" size="sm" variant="default" className="rounded-xl">
          <Send className="w-4 h-4" />
        </Button>
      </form>
    </div>
  );
};
