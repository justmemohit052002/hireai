import React from 'react';
import { cn } from '@/utils';

export const Card = React.forwardRef(
  ({ className, variant = 'surface', interactive = false, hoverable = false, glow = 'none', children, ...props }, ref) => {
    const isNested = props['surface-nested'] ?? props.nested ?? props.surfaceNested ?? false;
    const isGlass = variant === 'glass' || props.glass;
    const isInteractive = interactive || hoverable;
    const { 'surface-nested': _sn, nested: _n, surfaceNested: _snn, glass: _g, ...restProps } = props;

    const variantClass = isGlass
      ? 'glass-card'
      : (variant === 'nested' || isNested)
        ? 'surface-nested'
        : 'surface';

    return (
      <div
        ref={ref}
        className={cn(
          'p-6 transition-all duration-150',
          variantClass,
          isInteractive && 'interactive-card cursor-pointer',
          glow === 'primary' && 'focus-ring',
          className
        )}
        {...restProps}
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
  <div className={cn('flex items-center pt-4 border-t border-border', className)} {...props} />
);
