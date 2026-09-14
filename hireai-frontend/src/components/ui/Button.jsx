import React from 'react';
import { cva } from 'class-variance-authority';
import { cn } from '@/utils';

const buttonVariants = cva(
  'inline-flex items-center justify-center gap-2 rounded-xl text-sm font-medium transition-all duration-150 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-blue dark:focus-visible:ring-brand-accent focus-visible:ring-offset-1 disabled:pointer-events-none disabled:opacity-50 active:scale-[0.98] select-none cursor-pointer',
  {
    variants: {
      variant: {
        default: 'bg-[#F1FF62] text-[#112358] hover:bg-[#e6f545] dark:bg-[#F1FF62] dark:text-[#112358] dark:hover:bg-[#e6f545] font-semibold shadow-sm',
        primary: 'bg-[#F1FF62] text-[#112358] hover:bg-[#e6f545] dark:bg-[#F1FF62] dark:text-[#112358] dark:hover:bg-[#e6f545] font-semibold shadow-sm',
        gradient: 'bg-[#004499] hover:bg-[#112358] text-[#F4F5FA] dark:bg-[#004499] dark:hover:bg-[#003380] dark:text-[#F4F5FA] shadow-sm font-semibold',
        secondary: 'bg-[#D4EAFF] text-[#004499] hover:bg-[#bce0ff] dark:bg-[#004499] dark:text-[#F4F5FA] dark:hover:bg-[#003880] font-medium shadow-sm',
        accent: 'bg-[#F1FF62] text-[#112358] hover:brightness-95 shadow-sm font-semibold',
        outline: 'bg-[#F4F5FA] border border-[#0192C6]/40 text-[#112358] hover:bg-[#D4EAFF]/50 dark:bg-[#112358] dark:border-[#004499] dark:text-[#F4F5FA] dark:hover:bg-[#004499]/40 font-medium',
        ghost: 'bg-transparent text-[#112358] dark:text-[#F4F5FA] hover:bg-[#D4EAFF]/50 dark:hover:bg-[#004499]/30 font-medium',
        'surface-nested': 'bg-[#D4EAFF] text-[#004499] hover:bg-[#bce0ff] dark:bg-[#004499] dark:text-[#F4F5FA] dark:hover:bg-[#003880] border border-[#0192C6]/20 dark:border-[#004499]/40 font-medium',
        danger: 'bg-red-600 text-white hover:bg-red-700 dark:bg-red-700 dark:text-white dark:hover:bg-red-800 font-medium shadow-sm',
      },
      size: {
        default: 'h-10 px-4 py-2',
        sm: 'h-8 px-3 text-xs rounded-lg',
        md: 'h-10 px-4 py-2',
        lg: 'h-11 px-5 text-base',
        icon: 'h-10 w-10 p-0',
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
