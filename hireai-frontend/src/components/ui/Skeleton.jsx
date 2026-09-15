import React from 'react';
import { cn } from '@/utils';

export function Skeleton({ className, ...props }) {
  return (
    <div
      className={cn('shimmer rounded-xl bg-surface-2/60 opacity-80', className)}
      {...props}
    />
  );
}
