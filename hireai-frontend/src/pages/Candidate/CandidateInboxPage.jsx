import React, { useEffect } from 'react';
import { ConversationList } from '@/features/messages/ConversationList';
import { ConversationView } from '@/features/messages/ConversationView';
import { Card } from '@/components/ui/Card';
import { useChat } from '@/context/ChatContext';
import { useAuth } from '@/context/AuthContext';
import { MessageSquare } from 'lucide-react';

export const CandidateInboxPage = () => {
  const { user } = useAuth();
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

  return (
    <div className="space-y-4">
      {/* Header Banner */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold font-heading text-foreground">Recruiter Conversations</h1>
          <p className="text-xs text-muted-foreground">Direct messaging with hiring teams regarding your applications and interview schedules</p>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 h-[calc(100vh-200px)] min-h-[550px]">
        {/* Left: Threads Sidebar */}
        <Card glass className="md:col-span-1 p-4 flex flex-col h-full overflow-hidden">
          <div className="flex items-center justify-between mb-3 px-1">
            <h3 className="text-xs font-bold uppercase tracking-wider text-muted-foreground flex items-center gap-1.5">
              <MessageSquare className="w-3.5 h-3.5 text-blue-500" />
              <span>Recruiter Threads ({conversations.length})</span>
            </h3>
          </div>
          <div className="flex-1 overflow-y-auto">
            <ConversationList
              conversations={conversations}
              activeId={activeConversationId}
              onSelect={selectConversation}
              currentUserId={user?.id}
            />
          </div>
        </Card>

        {/* Right: Active Chat Window */}
        <div className="md:col-span-2 h-full overflow-hidden">
          <ConversationView
            conversation={activeConversation}
            messages={activeMessages}
            currentUserId={user?.id}
            onSendMessage={sendMessage}
            onSendTyping={sendTyping}
            isPartnerTyping={isPartnerTyping}
            isLoadingMessages={isLoadingMessages}
          />
        </div>
      </div>
    </div>
  );
};
