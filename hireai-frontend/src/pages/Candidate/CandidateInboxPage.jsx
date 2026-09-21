import React, { useState, useEffect } from 'react';
import { ConversationList } from '@/features/messages/ConversationList';
import { ConversationView } from '@/features/messages/ConversationView';
import { Card } from '@/components/ui/Card';
import { useChat } from '@/context/ChatContext';
import { useAuth } from '@/context/AuthContext';
import { MessageSquare, Building2 } from 'lucide-react';

export const CandidateInboxPage = () => {
  const { user } = useAuth();
  const [mobileView, setMobileView] = useState('list'); // 'list' | 'chat'

  const {
    conversations,
    activeConversationId,
    activeConversation,
    activeMessages,
    isPartnerTyping,
    isLoadingMessages,
    selectConversation,
    sendMessage,
    sendTyping,
    refreshConversations,
  } = useChat();

  useEffect(() => {
    refreshConversations();
  }, [refreshConversations]);

  useEffect(() => {
    if (!activeConversationId && conversations.length > 0) {
      selectConversation(conversations[0].id);
    }
  }, [activeConversationId, conversations, selectConversation]);

  const handleSelectThread = (id) => {
    selectConversation(id);
    setMobileView('chat');
  };

  return (
    <div className="flex flex-col h-[calc(100dvh-120px)] md:h-[calc(100dvh-135px)] min-h-[480px] max-h-[calc(100dvh-120px)] overflow-hidden">
      {/* Compact Header Bar */}
      <div className="flex items-center justify-between shrink-0 mb-3 px-1">
        <div className="flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-xl bg-brand-blue/15 text-brand-blue flex items-center justify-center shrink-0">
            <MessageSquare className="w-4 h-4" />
          </div>
          <div>
            <h1 className="text-base sm:text-lg font-bold font-heading text-foreground leading-tight">
              Recruiter Conversations
            </h1>
            <p className="text-[11px] text-muted-foreground hidden sm:block">
              Direct messaging with hiring managers and interview schedule updates
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2 shrink-0">
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20 text-[11px] font-bold">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" />
            Live Chat
          </span>
        </div>
      </div>

      {/* Main Chat Grid (Master-Detail on mobile, 2-column on desktop) */}
      <div className="flex-1 min-h-0 flex gap-4 lg:gap-6 overflow-hidden">
        {/* Left: Recruiter Threads Sidebar */}
        <Card
          glass
          className={`p-3.5 sm:p-4 flex-col h-full min-h-0 overflow-hidden rounded-[24px] border border-border shadow-lg ${
            mobileView === 'chat'
              ? 'hidden md:flex md:w-80 lg:w-96 shrink-0'
              : 'flex w-full md:w-80 lg:w-96 shrink-0'
          }`}
        >
          <div className="flex items-center justify-between mb-2.5 px-1 shrink-0">
            <h3 className="text-xs font-bold uppercase tracking-wider text-brand-blue flex items-center gap-1.5">
              <Building2 className="w-3.5 h-3.5" />
              <span>Hiring Teams ({conversations.length})</span>
            </h3>
          </div>
          <div className="flex-1 min-h-0 overflow-hidden">
            <ConversationList
              conversations={conversations}
              activeId={activeConversationId}
              onSelect={handleSelectThread}
              currentUserId={user?.id}
            />
          </div>
        </Card>

        {/* Right: Active Chat Window */}
        <div
          className={`flex-1 min-w-0 h-full min-h-0 overflow-hidden ${
            mobileView === 'list' ? 'hidden md:flex flex-col' : 'flex flex-col'
          }`}
        >
          <ConversationView
            conversation={activeConversation}
            messages={activeMessages}
            currentUserId={user?.id}
            onSendMessage={sendMessage}
            onSendTyping={sendTyping}
            isPartnerTyping={isPartnerTyping}
            isLoadingMessages={isLoadingMessages}
            onBackToList={() => setMobileView('list')}
          />
        </div>
      </div>
    </div>
  );
};
