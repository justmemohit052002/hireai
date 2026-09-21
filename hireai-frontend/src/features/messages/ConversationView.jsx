import React, { useState, useEffect, useRef } from 'react';
import { Send, Paperclip, Sparkles, Building2, Calendar, Loader2, X } from 'lucide-react';
import { Avatar } from '@/components/ui/Avatar';
import { Button } from '@/components/ui/Button';
import { MessageBubble } from './MessageBubble';
import { chatApi } from '@/services/api/chat.api';
import { useAuth } from '@/context/AuthContext';

const RECRUITER_PROMPTS = [
  'Request availability for Round 1 technical interview',
  'What is your current notice period and CTC expectation?',
  'Could you share your GitHub or portfolio link?',
  'Sending the job description and assessment details',
];

const CANDIDATE_PROMPTS = [
  'I am available for an interview this week',
  'Thank you for considering my application',
  'Could you share more details about the interview format?',
  'I have attached my updated resume',
];

export const ConversationView = ({
  conversation,
  messages = [],
  currentUserId,
  onSendMessage,
  onSendTyping,
  isPartnerTyping = false,
  isLoadingMessages = false,
}) => {
  const { role } = useAuth();
  const isRecruiter = role === 'recruiter';
  const aiPrompts = isRecruiter ? RECRUITER_PROMPTS : CANDIDATE_PROMPTS;

  const [text, setText] = useState('');
  const [isUploading, setIsUploading] = useState(false);
  const [selectedFile, setSelectedFile] = useState(null);
  const messagesEndRef = useRef(null);
  const fileInputRef = useRef(null);
  const typingTimeoutRef = useRef(null);

  const partner = conversation?.partner || {};
  const partnerName = partner.firstName
    ? `${partner.firstName} ${partner.lastName || ''}`.trim()
    : conversation?.participantDetails?.find((p) => p.id !== currentUserId)?.name || (isRecruiter ? 'Candidate' : 'Hiring Team');
  const jobTitle = conversation?.jobTitle || conversation?.jobContext?.title;

  // Auto scroll to bottom
  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, isPartnerTyping]);

  const handleInputChange = (e) => {
    setText(e.target.value);

    // Trigger typing event
    if (onSendTyping) {
      onSendTyping(true);
      if (typingTimeoutRef.current) clearTimeout(typingTimeoutRef.current);
      typingTimeoutRef.current = setTimeout(() => {
        onSendTyping(false);
      }, 2000);
    }
  };

  const handleSend = async (e) => {
    e?.preventDefault();
    if (!text.trim() && !selectedFile) return;

    if (selectedFile && conversation?.id) {
      setIsUploading(true);
      try {
        const uploadRes = await chatApi.uploadAttachment(conversation.id, selectedFile);
        onSendMessage(text.trim(), {
          messageType: 'ATTACHMENT',
          attachmentUrl: uploadRes.url,
          attachmentName: uploadRes.name,
          attachmentSize: uploadRes.size,
        });
        setSelectedFile(null);
      } catch (err) {
        console.error('Failed to upload attachment', err);
      } finally {
        setIsUploading(false);
      }
    } else {
      onSendMessage(text.trim());
    }

    setText('');
    if (onSendTyping) onSendTyping(false);
  };

  const handleFileSelect = (e) => {
    const file = e.target.files?.[0];
    if (file) {
      setSelectedFile(file);
    }
  };

  const handleSendInterviewInvite = () => {
    onSendMessage('I would like to invite you to schedule a technical interview round.', {
      messageType: 'INTERVIEW_INVITE',
    });
  };

  const applyAiPrompt = (prompt) => {
    setText(prompt);
  };

  if (!conversation) {
    return (
      <div className="flex flex-col items-center justify-center h-full glass rounded-[24px] border border-border p-8 text-center text-muted-foreground">
        <div className="w-14 h-14 rounded-2xl bg-brand-blue-light/20 text-brand-blue flex items-center justify-center mb-4">
          <Sparkles className="w-7 h-7 animate-pulse" />
        </div>
        <h3 className="font-bold text-lg font-heading text-foreground">Select a conversation</h3>
        <p className="text-xs max-w-xs mt-1 text-muted-foreground">Choose a thread from the list to start messaging in real-time.</p>
      </div>
    );
  }

  return (
    <div className="flex flex-col h-full glass surface rounded-[24px] border border-border overflow-hidden shadow-xl">
      {/* Header */}
      <div className="flex items-center justify-between p-4 border-b border-border bg-surface-2/40 backdrop-blur-md">
        <div className="flex items-center gap-3">
          <Avatar name={partnerName} size="md" />
          <div>
            <h3 className="font-bold text-sm font-heading text-foreground">{partnerName}</h3>
            {jobTitle && (
              <p className="text-xs text-brand-blue font-semibold flex items-center gap-1 mt-0.5">
                <Building2 className="w-3 h-3" /> Re: {jobTitle}
              </p>
            )}
          </div>
        </div>

        <div className="flex items-center gap-2">
          {/* Only show Invite to Interview button to Recruiters */}
          {isRecruiter && (
            <Button
              variant="outline"
              size="sm"
              onClick={handleSendInterviewInvite}
              className="text-xs hidden sm:flex items-center gap-1.5 border-brand-blue/30 text-brand-blue hover:bg-brand-blue-light/20"
            >
              <Calendar className="w-3.5 h-3.5" />
              <span>Invite to Interview</span>
            </Button>
          )}
          <div className="flex items-center gap-1.5 px-3 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/20 text-xs font-bold text-emerald-500">
            <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
            <span>Live Chat</span>
          </div>
        </div>
      </div>

      {/* Message Feed */}
      <div className="flex-1 overflow-y-auto p-4 space-y-2 bg-background/20">
        {isLoadingMessages ? (
          <div className="flex justify-center items-center h-full">
            <Loader2 className="w-6 h-6 animate-spin text-brand-blue" />
          </div>
        ) : messages.length === 0 ? (
          <div className="text-center py-12 text-muted-foreground space-y-3">
            <div className="w-12 h-12 rounded-2xl bg-brand-blue-light/20 text-brand-blue flex items-center justify-center mx-auto">
              <Sparkles className="w-6 h-6" />
            </div>
            <p className="text-xs font-medium">No messages yet. Send a greeting to start the conversation!</p>
          </div>
        ) : (
          messages.map((msg) => (
            <MessageBubble
              key={msg.id}
              message={msg}
              isSelf={msg.senderId === currentUserId || msg.sender?.id === currentUserId}
              onSelectSlot={(slot) => {
                onSendMessage(`Confirmed: I have selected the ${slot} interview slot. Looking forward to our discussion!`);
              }}
            />
          ))
        )}

        {/* Typing Indicator Bubble */}
        {isPartnerTyping && (
          <div className="flex items-center gap-2 text-xs text-muted-foreground pl-2 py-1">
            <span className="flex gap-1 items-center px-3 py-1.5 rounded-2xl bg-surface-2 border border-border">
              <span className="w-1.5 h-1.5 rounded-full bg-brand-blue animate-bounce" />
              <span className="w-1.5 h-1.5 rounded-full bg-brand-blue animate-bounce [animation-delay:0.2s]" />
              <span className="w-1.5 h-1.5 rounded-full bg-brand-blue animate-bounce [animation-delay:0.4s]" />
            </span>
            <span className="text-[11px] font-medium">{partnerName} is typing...</span>
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>

      {/* AI Quick Prompt Chips */}
      <div className="px-4 py-2.5 border-t border-border bg-surface-2/30 flex items-center gap-2 overflow-x-auto no-scrollbar">
        <Sparkles className="w-3.5 h-3.5 text-brand-accent shrink-0" />
        <span className="text-[10px] font-bold uppercase tracking-wider text-muted-foreground shrink-0">Suggestions:</span>
        {aiPrompts.map((prompt, idx) => (
          <button
            key={idx}
            type="button"
            onClick={() => applyAiPrompt(prompt)}
            className="px-3 py-1 rounded-full text-[11px] bg-surface border border-border hover:border-brand-blue hover:bg-brand-blue-light/20 text-foreground transition-all shrink-0 font-medium cursor-pointer shadow-2xs"
          >
            {prompt}
          </button>
        ))}
      </div>

      {/* Selected File Badge */}
      {selectedFile && (
        <div className="px-4 py-2 bg-brand-blue-light/20 border-t border-brand-blue/30 flex items-center justify-between text-xs text-brand-navy dark:text-brand-blue-light font-medium">
          <span className="truncate">📎 Ready to send: {selectedFile.name}</span>
          <button type="button" onClick={() => setSelectedFile(null)} className="p-1 hover:bg-brand-blue/20 rounded-full cursor-pointer">
            <X className="w-3.5 h-3.5" />
          </button>
        </div>
      )}

      {/* Input Bar */}
      <form onSubmit={handleSend} className="p-3 border-t border-border bg-surface flex items-center gap-2">
        <input
          type="file"
          ref={fileInputRef}
          onChange={handleFileSelect}
          className="hidden"
          accept=".pdf,.doc,.docx,.png,.jpg,.jpeg"
        />
        <button
          type="button"
          onClick={() => fileInputRef.current?.click()}
          className="p-2 rounded-xl text-muted-foreground hover:text-brand-blue hover:bg-surface-2 transition-colors cursor-pointer"
          title="Attach Resume / Document"
        >
          <Paperclip className="w-4 h-4" />
        </button>

        <input
          type="text"
          value={text}
          onChange={handleInputChange}
          placeholder={`Message ${partnerName}...`}
          className="flex-1 bg-surface-2 border border-border focus:border-brand-blue focus:ring-2 focus:ring-brand-blue/20 rounded-xl px-3.5 py-2 text-sm text-foreground placeholder:text-muted-foreground transition-all outline-none"
        />

        <button
          type="submit"
          disabled={(!text.trim() && !selectedFile) || isUploading}
          className="btn-primary flex items-center justify-center gap-1.5 px-4 py-2 text-xs font-bold rounded-xl shadow-md cursor-pointer hover:scale-[1.02] active:scale-95 disabled:opacity-50 disabled:pointer-events-none transition-all"
        >
          {isUploading ? (
            <Loader2 className="w-4 h-4 animate-spin" />
          ) : (
            <>
              <span>Send</span>
              <Send className="w-3.5 h-3.5" />
            </>
          )}
        </button>
      </form>
    </div>
  );
};
