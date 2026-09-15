import React from 'react';
import * as TooltipPrimitive from '@radix-ui/react-tooltip';
import { cn } from '@/utils';

export const TooltipProvider = TooltipPrimitive.Provider;

export const Tooltip = ({ content, children, side = 'top' }) => {
  return (
    <TooltipProvider>
      <TooltipPrimitive.Root delayDuration={200}>
        <TooltipPrimitive.Trigger asChild>{children}</TooltipPrimitive.Trigger>
        <TooltipPrimitive.Content
          side={side}
          className={cn(
            'z-50 overflow-hidden rounded-lg glass border border-white/20 dark:border-white/10 px-3 py-1.5 text-xs text-foreground shadow-md animate-in fade-in-0 zoom-in-95'
          )}
        >
          {content}
        </TooltipPrimitive.Content>
      </TooltipPrimitive.Root>
    </TooltipProvider>
  );
};
