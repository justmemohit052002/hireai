import React, { useState } from 'react';
import { Sparkles, Search, SlidersHorizontal } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';

export const DynamicIslandSearch = ({
  onSearch,
  onFilterToggle,
}) => {
  const [query, setQuery] = useState('');
  const [isFocused, setIsFocused] = useState(false);

  return (
    <div className="relative flex justify-center w-full my-4">
      <motion.div
        layout
        transition={{ type: 'spring', stiffness: 400, damping: 30 }}
        className={`relative flex items-center gap-3 rounded-full glass border border-border px-4 py-2.5 shadow-2xl transition-all ${
          isFocused ? 'w-full max-w-2xl ring-2 ring-brand-blue/40 shadow-brand-blue/10' : 'w-full max-w-lg'
        }`}
      >
        {/* Input */}
        <input
          type="text"
          value={query}
          onFocus={() => setIsFocused(true)}
          onBlur={() => setIsFocused(false)}
          onChange={(e) => {
            setQuery(e.target.value);
            onSearch?.(e.target.value);
          }}
          placeholder="Search jobs by title, company, or keywords..."
          className="w-full bg-transparent text-sm text-foreground placeholder:text-muted-foreground focus:outline-none ml-2"
        />

        {/* Filter Toggle Button */}
        {onFilterToggle && (
          <button
            onClick={onFilterToggle}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-surface-2 hover:bg-muted text-xs font-semibold text-foreground transition-colors shrink-0 border border-border/50"
          >
            <SlidersHorizontal className="w-3.5 h-3.5 text-brand-blue" />
            <span className="hidden sm:inline">Filters</span>
          </button>
        )}

        {/* Search button */}
          <button
          onClick={() => onSearch?.(query)}
          className="p-2 rounded-full bg-brand-blue text-white hover:brightness-105 transition-all shrink-0 shadow-md shadow-brand-blue/20"
        >
          <Search className="w-4 h-4" />
        </button>
      </motion.div>
    </div>
  );
};
