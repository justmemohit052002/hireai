import React from 'react';
import { cva } from 'class-variance-authority';
import { cn } from '@/utils';

const badgeVariants = cva(
  'inline-flex items-center rounded-md px-2.5 py-0.5 text-xs font-semibold transition-colors focus:outline-none focus:ring-2 focus:ring-brand-blue select-none',
  {
    variants: {
      variant: {
        default: 'border border-brand-blue/30 bg-brand-blue/10 text-brand-blue dark:text-brand-blue-light',
        secondary: 'surface-nested border border-border text-foreground',
        accent: 'border border-brand-accent/40 bg-brand-accent/20 text-brand-dark dark:text-brand-accent',
        brand: 'badge-highlight',
        warning: 'border border-amber-500/30 bg-amber-500/10 text-amber-600 dark:text-amber-400',
        danger: 'border border-red-500/30 bg-red-500/10 text-red-600 dark:text-red-400',
        success: 'border border-emerald-500/30 bg-emerald-500/10 text-emerald-600 dark:text-emerald-400',
        outline: 'border border-border text-foreground bg-surface',
        'surface-nested': 'surface-nested text-foreground',
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
