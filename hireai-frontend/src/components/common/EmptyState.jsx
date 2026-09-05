import React from 'react';
import { SearchX } from 'lucide-react';
import { Button } from '@/components/ui/Button';

export const EmptyState = ({
  icon = <SearchX className="w-12 h-12 text-muted-foreground stroke-[1.5]" />,
  title,
  description,
  actionLabel,
  onAction,
}) => {
  return (
    <div className="flex flex-col items-center justify-center text-center p-12 glass rounded-[24px] border border-white/10 my-6">
      <div className="p-4 rounded-2xl bg-muted/50 mb-4 border border-border/50 text-foreground">
        {icon}
      </div>
      <h3 className="text-xl font-bold font-heading text-foreground mb-2">{title}</h3>
      <p className="text-sm text-muted-foreground max-w-md leading-relaxed mb-6">
        {description}
      </p>
      {actionLabel && onAction && (
        <Button onClick={onAction} variant="default" size="default">
          {actionLabel}
        </Button>
      )}
    </div>
  );
};
