import React from 'react';
import { Card } from '@/components/ui/Card';
import { cn } from '@/utils';

export const StatsCard = ({
  title,
  value,
  change,
  isPositive = true,
  icon,
}) => {
  return (
    <Card className="p-6 border border-border/70 rounded-2xl shadow-xs surface-nested bg-surface/70">
      <div className="flex items-center justify-between">
        <span className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
          {title}
        </span>
        <div className="p-2.5 rounded-xl bg-surface-2 text-foreground border border-border/40">
          {icon}
        </div>
      </div>

      <div className="mt-4 flex items-baseline justify-between">
        <span className="text-3xl font-bold font-heading text-foreground">
          {value}
        </span>
        {change && (
          <span
            className={cn(
              'text-[12px] font-bold px-2.5 py-1 rounded-lg border',
              isPositive
                ? 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/20'
                : 'bg-red-500/10 text-red-600 dark:text-red-400 border-red-500/20'
            )}
          >
            {change}
          </span>
        )}
      </div>
    </Card>
  );
};
