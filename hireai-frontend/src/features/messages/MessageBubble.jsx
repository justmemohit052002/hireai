import React, { useState } from 'react';
import { Check, CheckCheck, FileText, Download, Calendar, Sparkles } from 'lucide-react';
import { cn, formatRelativeTime } from '@/utils';

export const MessageBubble = ({ message, isSelf, onSelectSlot }) => {
  const [selectedSlot, setSelectedSlot] = useState(null);

  const text = message.messageText || message.content;
  const createdAt = message.createdAt || message.sentAt || new Date().toISOString();
  const isAttachment = message.messageType === 'ATTACHMENT' || Boolean(message.attachmentUrl);
  const isInterviewInvite = message.messageType === 'INTERVIEW_INVITE';
  const isSystem = message.messageType === 'SYSTEM';

  const handleSlotClick = (slot) => {
    if (isSelf || selectedSlot) return;
    setSelectedSlot(slot);
    if (onSelectSlot) {
      onSelectSlot(slot);
    }
  };

  if (isSystem) {
    return (
      <div className="flex justify-center my-3">
        <div className="px-3.5 py-1.5 rounded-full bg-surface-2 border border-border text-xs text-muted-foreground flex items-center gap-1.5 shadow-2xs">
          <Sparkles className="w-3.5 h-3.5 text-brand-blue" />
          <span>{text}</span>
        </div>
      </div>
    );
  }

  return (
    <div className={cn('flex flex-col mb-3 max-w-[82%]', isSelf ? 'ml-auto items-end' : 'mr-auto items-start')}>
      <div
        className={cn(
          'p-3.5 rounded-2xl text-sm leading-relaxed shadow-sm transition-all',
          isSelf
            ? 'bg-brand-blue text-white rounded-br-xs font-medium'
            : 'bg-surface border border-border text-foreground rounded-bl-xs'
        )}
      >
        {/* Regular Message Text */}
        {text && <p className="whitespace-pre-wrap break-words">{text}</p>}

        {/* Attachment Card */}
        {isAttachment && (
          <div className={cn(
            'flex items-center gap-3 p-2.5 mt-2 rounded-xl border',
            isSelf
              ? 'bg-white/10 border-white/20 text-white'
              : 'bg-surface-2 border-border text-foreground'
          )}>
            <div className={cn(
              'p-2 rounded-lg',
              isSelf ? 'bg-white/20 text-white' : 'bg-brand-blue-light/30 text-brand-blue'
            )}>
              <FileText className="w-5 h-5" />
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-xs font-semibold truncate">{message.attachmentName || 'Attachment'}</p>
              {message.attachmentSize && (
                <p className="text-[10px] opacity-75">{(message.attachmentSize / 1024).toFixed(1)} KB</p>
              )}
            </div>
            {message.attachmentUrl && (
              <a
                href={message.attachmentUrl}
                target="_blank"
                rel="noreferrer"
                download
                className="p-1.5 rounded-lg hover:bg-black/10 dark:hover:bg-white/20 transition-colors"
                title="Download Attachment"
              >
                <Download className="w-4 h-4" />
              </a>
            )}
          </div>
        )}

        {/* Interview Invitation Card */}
        {isInterviewInvite && (
          <div className={cn(
            'p-3.5 mt-2.5 rounded-xl border space-y-2.5',
            isSelf
              ? 'bg-white/10 border-white/20 text-white'
              : 'bg-surface-2 border-brand-blue/30 text-foreground shadow-xs'
          )}>
            <div className="flex items-center justify-between">
              <div className={cn(
                'flex items-center gap-1.5 text-xs font-bold',
                isSelf ? 'text-white' : 'text-brand-blue'
              )}>
                <Calendar className="w-4 h-4" />
                <span>Interview Invitation</span>
              </div>
              {isSelf && (
                <span className="text-[10px] opacity-75 italic font-normal">Sent to Candidate</span>
              )}
            </div>

            <p className="text-xs opacity-90 leading-normal">
              {isSelf
                ? 'Candidate has been invited to select one of the following available interview slots:'
                : 'Please select an available slot for your technical evaluation round:'}
            </p>

            {selectedSlot ? (
              <div className="flex items-center gap-2 p-2.5 rounded-xl bg-emerald-500/15 border border-emerald-500/30 text-emerald-500 text-xs font-bold">
                <CheckCheck className="w-4 h-4 shrink-0 text-emerald-500" />
                <span>Slot Confirmed: {selectedSlot}</span>
              </div>
            ) : (
              <div className="grid grid-cols-2 gap-2 pt-1">
                <button
                  type="button"
                  onClick={() => handleSlotClick('Tomorrow 11:00 AM')}
                  disabled={isSelf}
                  className={cn(
                    'px-3 py-2 rounded-xl text-xs font-bold text-center transition-all border cursor-pointer',
                    isSelf
                      ? 'bg-white/5 border-white/15 opacity-80 cursor-default'
                      : 'btn-primary shadow-xs hover:scale-[1.02] active:scale-95'
                  )}
                >
                  Tomorrow 11:00 AM
                </button>
                <button
                  type="button"
                  onClick={() => handleSlotClick('Tomorrow 3:30 PM')}
                  disabled={isSelf}
                  className={cn(
                    'px-3 py-2 rounded-xl text-xs font-bold text-center transition-all border cursor-pointer',
                    isSelf
                      ? 'bg-white/5 border-white/15 opacity-80 cursor-default'
                      : 'btn-primary shadow-xs hover:scale-[1.02] active:scale-95'
                  )}
                >
                  Tomorrow 3:30 PM
                </button>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Timestamp & Delivery Status */}
      <div className="flex items-center gap-1 mt-1 px-1 text-[10px] text-muted-foreground">
        <span>{formatRelativeTime(createdAt)}</span>
        {isSelf && (
          <>
            <span>•</span>
            {message.status === 'READ' ? (
              <span className="flex items-center text-brand-blue font-semibold" title="Read">
                <CheckCheck className="w-3 h-3 stroke-[2.5]" />
              </span>
            ) : (
              <span className="flex items-center opacity-70" title="Sent">
                <Check className="w-3 h-3 stroke-[2]" />
              </span>
            )}
          </>
        )}
      </div>
    </div>
  );
};
