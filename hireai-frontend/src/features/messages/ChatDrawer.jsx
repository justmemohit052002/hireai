import React, { useEffect } from 'react';
import { X, MessageSquare, Sparkles } from 'lucide-react';
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
    <div className="fixed inset-0 z-50 overflow-hidden flex justify-end bg-black/40 backdrop-blur-xs animate-in fade-in duration-200">
      <div className="w-full max-w-xl bg-background border-l border-border h-full flex flex-col shadow-2xl animate-in slide-in-from-right duration-300">
        {/* Drawer Header */}
        <div className="p-4 border-b border-border flex items-center justify-between bg-muted/30">
          <div className="flex items-center gap-2">
            <div className="p-2 rounded-xl bg-blue-500/10 text-blue-500">
              <MessageSquare className="w-5 h-5" />
            </div>
            <div>
              <h2 className="font-bold text-sm text-foreground">
                Chat with {candidate?.name || candidate?.firstName || 'Candidate'}
              </h2>
              {job?.title && (
                <p className="text-xs text-muted-foreground">Re: {job.title}</p>
              )}
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-xl text-muted-foreground hover:text-foreground hover:bg-muted transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Drawer Chat Body */}
        <div className="flex-1 p-3 overflow-hidden">
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
