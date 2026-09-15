import React from 'react';
import { Search, X } from 'lucide-react';
import { Input } from '@/components/ui/Input';

export const SearchBar = ({
  value,
  onChange,
  placeholder = 'Search jobs, skills, companies...',
  className,
  onClear,
}) => {
  return (
    <Input
      type="text"
      value={value}
      onChange={(e) => onChange(e.target.value)}
      placeholder={placeholder}
      className={className}
      icon={<Search className="w-4 h-4 text-muted-foreground" />}
      rightElement={
        value ? (
          <button
            onClick={() => {
              onChange('');
              onClear?.();
            }}
            className="rounded-full p-1 hover:bg-muted text-muted-foreground hover:text-foreground transition-colors"
          >
            <X className="w-3.5 h-3.5" />
          </button>
        ) : undefined
      }
    />
  );
};
