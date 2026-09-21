import React from 'react';
import { cva } from 'class-variance-authority';
import { cn } from '@/utils';

const badgeVariants = cva(
  'inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold transition-colors focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 border select-none',
  {
    variants: {
      variant: {
        default: 'badge-highlight border-transparent',
        secondary: 'badge-highlight border-transparent',
        accent: 'badge-highlight border-transparent',
        brand: 'badge-highlight border-transparent',
        warning: 'border-amber-500/25 bg-amber-500/10 text-amber-600 dark:text-amber-400',
        danger: 'border-red-500/25 bg-red-500/10 text-red-600 dark:text-red-400',
        outline: 'border-border text-foreground bg-surface-2',
        glass: 'glass text-foreground border-white/20 dark:border-white/10',
      },
    },
    defaultVariants: {
      variant: 'default',
    },
  }
);

export function Badge({ className, variant, ...props }) {
  return <div className={cn(badgeVariants({ variant }), className)} {...props} />;
}
