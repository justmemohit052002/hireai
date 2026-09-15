import React from 'react';
import { cva } from 'class-variance-authority';
import { cn } from '@/utils';

const buttonVariants = cva(
  'inline-flex items-center justify-center gap-2 rounded-xl text-sm font-medium transition-all duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#F56681] disabled:pointer-events-none disabled:opacity-50 active:scale-[0.98] select-none cursor-pointer',
  {
    variants: {
      variant: {
        default: 'bg-[#22214B] text-white shadow-md shadow-[#22214B]/20 hover:bg-[#1A193B] dark:bg-[#F56681] dark:text-white dark:hover:bg-[#E4536E]',
        gradient: 'bg-gradient-to-r from-[#C63FC5] via-[#F56681] to-[#FC9559] text-white shadow-md shadow-[#C63FC5]/20 hover:brightness-105 hover:shadow-lg hover:shadow-[#C63FC5]/30',
        secondary: 'bg-[#F56681] text-white shadow-md shadow-[#F56681]/20 hover:bg-[#E4536E]',
        accent: 'bg-[#FC9559] text-white shadow-md shadow-[#FC9559]/20 hover:bg-[#EA8347]',
        outline: 'border border-border bg-background/50 hover:bg-muted hover:border-[#F56681]/40 hover:text-foreground backdrop-blur-md',
        ghost: 'hover:bg-muted hover:text-foreground',
        glass: 'glass hover:bg-white/20 dark:hover:bg-slate-800/60 text-foreground border border-white/20 dark:border-white/10',
        danger: 'bg-red-600 text-white hover:bg-red-500 shadow-md shadow-red-500/20',
      },
      size: {
        default: 'h-10 px-4 py-2',
        sm: 'h-8 px-3 text-xs rounded-lg',
        lg: 'h-12 px-6 text-base rounded-2xl',
        icon: 'h-10 w-10 p-0 rounded-xl',
      },
    },
    defaultVariants: {
      variant: 'default',
      size: 'default',
    },
  }
);

export const Button = React.forwardRef(
  ({ className, variant, size, isLoading, children, disabled, ...props }, ref) => {
    return (
      <button
        className={cn(buttonVariants({ variant, size, className }))}
        ref={ref}
        disabled={disabled || isLoading}
        {...props}
      >
        {isLoading && (
          <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-current fill-none" viewBox="0 0 24 24">
            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
          </svg>
        )}
        {children}
      </button>
    );
  }
);
Button.displayName = 'Button';
