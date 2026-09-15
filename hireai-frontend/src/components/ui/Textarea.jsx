import React from 'react';
import { cn } from '@/utils';

export const Textarea = React.forwardRef(
  ({ className, error, ...props }, ref) => {
    return (
      <div className="w-full">
        <textarea
          className={cn(
            'flex min-h-[100px] w-full rounded-xl border border-border bg-surface-2/80 dark:bg-surface-2 text-foreground px-4 py-3 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#F56681]/40 focus-visible:border-[#F56681] disabled:cursor-not-allowed disabled:opacity-50 transition-all duration-200 shadow-xs resize-y',
            error && 'border-red-500 focus-visible:ring-red-500',
            className
          )}
          ref={ref}
          {...props}
        />
        {error && <p className="mt-1.5 text-xs text-red-500 font-medium">{error}</p>}
      </div>
    );
  }
);
Textarea.displayName = 'Textarea';
