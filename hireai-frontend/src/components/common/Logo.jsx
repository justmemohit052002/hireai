import React from 'react';
import { cn } from '@/utils';

/**
 * Minimal, modern HireAI Brand Logo
 * Geometric AI neural node + H letter mark with gradient glow.
 */
export const Logo = ({ className, iconOnly = false, size = 'md' }) => {
  const sizeMap = {
    sm: { icon: 'w-7 h-7', text: 'text-lg', badge: 'text-[9px] px-1.5 py-0.5' },
    md: { icon: 'w-9 h-9', text: 'text-xl', badge: 'text-[10px] px-2 py-0.5' },
    lg: { icon: 'w-11 h-11', text: 'text-2xl', badge: 'text-xs px-2.5 py-1' },
  };

  const currentSize = sizeMap[size];

  return (
    <div className={cn('inline-flex items-center gap-2.5 select-none font-bold tracking-tight', className)}>
      {/* Icon Mark */}
      <div className={cn('relative flex items-center justify-center rounded-xl bg-brand-navy text-foreground text-foreground shadow-md shadow-[#C63FC5]/20 ring-1 ring-white/20', currentSize.icon)}>
        {/* Abstract H + AI spark */}
        <svg viewBox="0 0 24 24" fill="none" className="w-5 h-5 text-foreground stroke-[2.2]" stroke="currentColor">
          <path d="M6 5v14M18 5v14M6 12h12" strokeLinecap="round" />
          <circle cx="12" cy="12" r="2.5" fill="white" className="animate-pulse" />
        </svg>
        {/* Corner glow dot */}
        <span className="absolute -top-0.5 -right-0.5 w-2 h-2 rounded-full bg-[#FC9559] ring-2 ring-background" />
      </div>

      {/* Wordmark */}
      {!iconOnly && (
        <div className="flex items-center gap-1.5 font-heading">
          <span className={cn('font-bold tracking-tight text-foreground', currentSize.text)}>
            Hire<span className="bg-brand-blue bg-clip-text text-transparent">AI</span>
          </span>
          <span className={cn('rounded-full bg-brand-blue-light/10 text-[#F56681] border border-[#F56681]/20 font-semibold font-sans uppercase tracking-widest', currentSize.badge)}>
            PRO
          </span>
        </div>
      )}
    </div>
  );
};
