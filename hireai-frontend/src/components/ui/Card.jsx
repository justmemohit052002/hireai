import React from 'react';
import { cn } from '@/utils';

export const Card = React.forwardRef(
  ({ className, glass = true, hoverable = false, glow = 'none', children, ...props }, ref) => {
    const glowClasses = {
      none: '',
      primary: 'glow-primary',
      secondary: 'glow-secondary',
      accent: 'glow-accent',
    };

    return (
      <div
        ref={ref}
        className={cn(
          'rounded-[18px] p-6 transition-all duration-200 border',
          glass
            ? 'glass border-border shadow-sm'
            : 'bg-surface border-border shadow-sm',
          hoverable && 'hover-lift cursor-pointer hover:border-brand-blue/40',
          glowClasses[glow],
          className
        )}
        {...props}
      >
        {children}
      </div>
    );
  }
);
Card.displayName = 'Card';


export const CardHeader = ({ className, ...props }) => (
  <div className={cn('flex flex-col space-y-1.5 pb-4', className)} {...props} />
);

export const CardTitle = ({ className, ...props }) => (
  <h3 className={cn('text-xl font-bold tracking-tight font-heading text-foreground', className)} {...props} />
);

export const CardDescription = ({ className, ...props }) => (
  <p className={cn('text-sm text-muted-foreground', className)} {...props} />
);

export const CardContent = ({ className, ...props }) => (
  <div className={cn('pt-0', className)} {...props} />
);

export const CardFooter = ({ className, ...props }) => (
  <div className={cn('flex items-center pt-4 border-t border-border/50', className)} {...props} />
);
