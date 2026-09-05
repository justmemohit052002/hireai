import React from 'react';
import * as SelectPrimitive from '@radix-ui/react-select';
import { Check, ChevronDown } from 'lucide-react';
import { cn } from '@/utils';

export const Select = ({
  value,
  onChange,
  options,
  placeholder = 'Select option...',
  className,
  error,
}) => {
  const handleValueChange = (val) => {
    if (!onChange) return;
    try {
      onChange(val);
    } catch {
      onChange({ target: { value: val, name: '' } });
    }
  };

  return (
    <div className="w-full">
      <SelectPrimitive.Root value={value} onValueChange={handleValueChange}>
        <SelectPrimitive.Trigger
          className={cn(
            'flex h-11 w-full items-center justify-between rounded-xl border border-border bg-surface-2/80 dark:bg-surface-2 text-foreground px-4 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-[#F56681]/40 focus:border-[#F56681] disabled:cursor-not-allowed disabled:opacity-50 transition-all duration-200 shadow-xs cursor-pointer',
            error && 'border-red-500 focus:ring-red-500',
            className
          )}
        >
          <SelectPrimitive.Value placeholder={placeholder} />
          <SelectPrimitive.Icon asChild>
            <ChevronDown className="h-4 w-4 text-muted-foreground" />
          </SelectPrimitive.Icon>
        </SelectPrimitive.Trigger>

        <SelectPrimitive.Portal>
          <SelectPrimitive.Content className="relative z-50 min-w-[8rem] overflow-hidden rounded-xl border border-border bg-surface text-foreground shadow-2xl backdrop-blur-xl animate-in fade-in-80">
            <SelectPrimitive.Viewport className="p-1">
              {options.map((opt) => (
                <SelectPrimitive.Item
                  key={opt.value}
                  value={opt.value}
                  className="relative flex w-full cursor-pointer select-none items-center rounded-lg py-2 pl-8 pr-4 text-sm text-foreground outline-none focus:bg-[#F56681]/15 focus:text-[#F56681] hover:bg-muted data-[disabled]:pointer-events-none data-[disabled]:opacity-50 transition-colors"
                >
                  <span className="absolute left-2 flex h-3.5 w-3.5 items-center justify-center">
                    <SelectPrimitive.ItemIndicator>
                      <Check className="h-4 w-4 text-[#F56681]" />
                    </SelectPrimitive.ItemIndicator>
                  </span>
                  <SelectPrimitive.ItemText>{opt.label}</SelectPrimitive.ItemText>
                </SelectPrimitive.Item>
              ))}
            </SelectPrimitive.Viewport>
          </SelectPrimitive.Content>
        </SelectPrimitive.Portal>
      </SelectPrimitive.Root>
      {error && <p className="mt-1.5 text-xs text-red-500 font-medium">{error}</p>}
    </div>
  );
};
