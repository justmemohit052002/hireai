import React, { useState } from 'react';
import { MOCK_CONVERSATIONS, MOCK_MESSAGES } from '@/mock';
import { ConversationList } from '@/features/messages/ConversationList';
import { ConversationView } from '@/features/messages/ConversationView';
import { Card } from '@/components/ui/Card';

export const CandidateInboxPage = () => {
  const [activeConvId, setActiveConvId] = useState(MOCK_CONVERSATIONS[0].id);
  const [messages, setMessages] = useState(MOCK_MESSAGES);

  const activeConv = MOCK_CONVERSATIONS.find((c) => c.id === activeConvId) || MOCK_CONVERSATIONS[0];
  const activeMessages = messages.filter((m) => m.conversationId === activeConv.id);

  const handleSend = (content) => {
    const newMsg = {
      id: `msg-${Date.now()}`,
      conversationId: activeConv.id,
      senderId: 'cand-1',
      content,
      sentAt: new Date().toISOString(),
      read: true,
    };
    setMessages((prev) => [...prev, newMsg]);
  };

  return (
    <div className="grid grid-cols-1 md:grid-cols-3 gap-6 h-[calc(100vh-140px)]">
      <Card surface-nested className="md:col-span-1 p-4 overflow-y-auto">
        <h3 className="text-sm font-bold uppercase tracking-wider text-muted-foreground mb-3 px-2">
          Messages & Threads
        </h3>
        <ConversationList
          conversations={MOCK_CONVERSATIONS}
          activeId={activeConvId}
          onSelect={(id) => setActiveConvId(id)}
          currentUserId="cand-1"
        />
      </Card>

      <div className="md:col-span-2 h-full">
        <ConversationView
          conversation={activeConv}
          messages={activeMessages}
          currentUserId="cand-1"
          onSendMessage={handleSend}
        />
      </div>
    </div>
  );
};
