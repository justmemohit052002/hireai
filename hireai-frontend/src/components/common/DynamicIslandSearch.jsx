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
        className={`relative flex items-center gap-3 rounded-full surface-nested border border-white/20 dark:border-white/10 px-4 py-2.5 shadow-2xl transition-all ${
          isFocused ? 'w-full max-w-2xl ring-2 ring-[#F56681]/50 shadow-[#F56681]/10' : 'w-full max-w-lg'
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
            <SlidersHorizontal className="w-3.5 h-3.5 text-[#F56681]" />
            <span className="hidden sm:inline">Filters</span>
          </button>
        )}

        {/* Search button */}
        <button
          onClick={() => onSearch?.(query)}
          className="p-2 rounded-full bg-gradient-to-r from-[#C63FC5] via-[#F56681] to-[#FC9559] hover:brightness-105 text-white transition-all shrink-0 shadow-md shadow-[#C63FC5]/20"
        >
          <Search className="w-4 h-4" />
        </button>
      </motion.div>
    </div>
  );
};
