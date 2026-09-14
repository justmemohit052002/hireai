import React, { useState } from 'react';
import { Eye, EyeOff } from 'lucide-react';
import { cn } from '@/utils';

export const Input = React.forwardRef(
  ({ className, type = 'text', icon, rightElement, error, ...props }, ref) => {
    const [showPassword, setShowPassword] = useState(false);
    const isPasswordType = type === 'password';
    const actualType = isPasswordType ? (showPassword ? 'text' : 'password') : type;

    const passwordToggleElement = isPasswordType ? (
      <button
        type="button"
        onClick={() => setShowPassword((prev) => !prev)}
        className="p-1 rounded-md text-muted-foreground hover:text-foreground hover:bg-muted transition-colors focus:outline-none"
        tabIndex={-1}
        aria-label={showPassword ? 'Hide password' : 'Show password'}
      >
        {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
      </button>
    ) : null;

    const effectiveRightElement = rightElement ?? passwordToggleElement;

    return (
      <div className="w-full">
        <div className="relative flex items-center">
          {icon && (
            <div className="absolute left-3 text-muted-foreground flex items-center justify-center pointer-events-none">
              {icon}
            </div>
          )}
          <input
            type={actualType}
            className={cn(
              'flex h-11 w-full rounded-lg border border-border bg-surface text-foreground px-3.5 py-2 text-sm transition-colors file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-blue dark:focus-visible:ring-brand-accent focus-visible:border-transparent disabled:cursor-not-allowed disabled:opacity-50 shadow-xs',
              icon && 'pl-10',
              effectiveRightElement && 'pr-10',
              error && 'border-red-500 focus-visible:ring-red-500',
              className
            )}
            ref={ref}
            {...props}
          />
          {effectiveRightElement && (
            <div className="absolute right-3 text-muted-foreground flex items-center justify-center">
              {effectiveRightElement}
            </div>
          )}
        </div>
        {error && <p className="mt-1.5 text-xs text-red-500 font-medium">{error}</p>}
      </div>
    );
  }
);
Input.displayName = 'Input';
