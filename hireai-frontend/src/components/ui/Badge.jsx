import React from 'react';
import { cva } from 'class-variance-authority';
import { cn } from '@/utils';

const badgeVariants = cva(
  'inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold transition-colors focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 border select-none',
  {
    variants: {
      variant: {
        default: 'border-[#C63FC5]/25 bg-[#C63FC5]/10 text-[#C63FC5] dark:text-[#E46BE3]',
        secondary: 'border-[#F56681]/25 bg-[#F56681]/10 text-[#F56681] dark:text-[#F88EA3]',
        accent: 'border-[#FC9559]/25 bg-[#FC9559]/10 text-[#D96F30] dark:text-[#FCAE7D]',
        brand: 'bg-gradient-to-r from-[#C63FC5] via-[#F56681] to-[#FC9559] text-white border-transparent shadow-xs',
        warning: 'border-amber-500/25 bg-amber-500/10 text-amber-600 dark:text-amber-400',
        danger: 'border-red-500/25 bg-red-500/10 text-red-600 dark:text-red-400',
        outline: 'border-border text-foreground bg-background/50',
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
