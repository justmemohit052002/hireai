import React, { useEffect } from 'react';
import { X, MessageSquare } from 'lucide-react';
import { useChat } from '@/context/ChatContext';
import { useAuth } from '@/context/AuthContext';
import { ConversationView } from './ConversationView';

export const ChatDrawer = ({ isOpen, onClose, candidate, job, jobApplication }) => {
  const { user } = useAuth();
  const {
    activeConversation,
    activeMessages,
    isPartnerTyping,
    isLoadingMessages,
    sendMessage,
    sendTyping,
    startConversation,
  } = useChat();

  useEffect(() => {
    if (isOpen && candidate?.id) {
      startConversation({
        candidateId: candidate.userId || candidate.id,
        jobId: job?.id,
        jobApplicationId: jobApplication?.id,
      });
    }
  }, [isOpen, candidate, job, jobApplication, startConversation]);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 overflow-hidden flex justify-end bg-black/50 backdrop-blur-xs animate-in fade-in duration-200">
      <div className="w-full max-w-xl bg-surface border-l border-border h-full flex flex-col shadow-2xl animate-in slide-in-from-right duration-300">
        {/* Drawer Header */}
        <div className="p-4 border-b border-border flex items-center justify-between bg-surface-2/60">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-brand-blue-light/25 text-brand-blue flex items-center justify-center">
              <MessageSquare className="w-4 h-4" />
            </div>
            <div>
              <h2 className="font-bold text-sm font-heading text-foreground">
                Chat with {candidate?.name || candidate?.firstName || 'Candidate'}
              </h2>
              {job?.title && (
                <p className="text-xs text-brand-blue font-medium">Re: {job.title}</p>
              )}
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-xl text-muted-foreground hover:text-foreground hover:bg-surface-2 transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Drawer Chat Body */}
        <div className="flex-1 p-3 overflow-hidden bg-background/30">
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
