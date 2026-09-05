import React from 'react';
import { SlidersHorizontal, RotateCcw } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { JOB_TYPES, EXPERIENCE_LEVELS } from '@/constants';

export const FilterPanel = ({ filters, onChange, onReset }) => {
  const toggleJobType = (type) => {
    const current = filters.jobType || [];
    const next = current.includes(type)
      ? current.filter((t) => t !== type)
      : [...current, type];
    onChange({ ...filters, jobType: next });
  };

  const toggleLevel = (level) => {
    const current = filters.level || [];
    const next = current.includes(level)
      ? current.filter((l) => l !== level)
      : [...current, level];
    onChange({ ...filters, level: next });
  };

  return (
    <div className="glass rounded-[24px] border border-white/20 dark:border-white/10 p-5 space-y-6 shadow-xl">
      {/* Title */}
      <div className="flex items-center justify-between pb-3 border-b border-border/50">
        <div className="flex items-center gap-2 font-bold text-sm font-heading text-foreground">
          <SlidersHorizontal className="w-4 h-4 text-blue-500" />
          <span>Filters</span>
        </div>
        <button
          onClick={onReset}
          className="text-xs text-muted-foreground hover:text-foreground flex items-center gap-1 font-medium"
        >
          <RotateCcw className="w-3 h-3" /> Reset
        </button>
      </div>

      {/* Job Type Filter */}
      <div className="space-y-2.5">
        <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground">
          Job Type
        </label>
        <div className="flex flex-wrap gap-2">
          {JOB_TYPES.map((jt) => {
            const isSelected = filters.jobType?.includes(jt.value);
            return (
              <button
                key={jt.value}
                onClick={() => toggleJobType(jt.value)}
                className={`px-3 py-1.5 rounded-xl text-xs font-semibold transition-all border ${
                  isSelected
                    ? 'bg-blue-600 text-white border-blue-600 shadow-md shadow-blue-500/20'
                    : 'bg-surface-2/60 text-muted-foreground border-border/60 hover:border-blue-500/40'
                }`}
              >
                {jt.label}
              </button>
            );
          })}
        </div>
      </div>

      {/* Experience Level */}
      <div className="space-y-2.5">
        <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground">
          Experience Level
        </label>
        <div className="flex flex-wrap gap-2">
          {EXPERIENCE_LEVELS.map((el) => {
            const isSelected = filters.level?.includes(el.value);
            return (
              <button
                key={el.value}
                onClick={() => toggleLevel(el.value)}
                className={`px-3 py-1.5 rounded-xl text-xs font-semibold transition-all border ${
                  isSelected
                    ? 'bg-indigo-600 text-white border-indigo-600 shadow-md shadow-indigo-500/20'
                    : 'bg-surface-2/60 text-muted-foreground border-border/60 hover:border-indigo-500/40'
                }`}
              >
                {el.label}
              </button>
            );
          })}
        </div>
      </div>

      {/* Location Filter */}
      <div className="space-y-2.5">
        <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground">
          Location
        </label>
        <input
          type="text"
          value={filters.location || ''}
          onChange={(e) => onChange({ ...filters, location: e.target.value })}
          placeholder="e.g. San Francisco, Remote"
          className="w-full px-3.5 py-2 rounded-xl text-sm bg-surface-2/60 text-foreground border border-border/60 placeholder:text-muted-foreground focus:outline-none focus:ring-1 focus:ring-blue-500 focus:border-blue-500 transition-all"
        />
      </div>

      {/* Minimum Salary Filter */}
      <div className="space-y-2.5">
        <label className="block text-xs font-bold uppercase tracking-wider text-muted-foreground">
          Minimum Salary (Annual)
        </label>
        <div className="flex flex-wrap gap-2">
          {[
            { label: 'Any', value: undefined },
            { label: '$50k+', value: 50000 },
            { label: '$100k+', value: 100000 },
            { label: '$150k+', value: 150000 },
            { label: '$200k+', value: 200000 },
          ].map((sal) => {
            const isSelected = filters.salaryMin === sal.value;
            return (
              <button
                key={sal.label}
                onClick={() => onChange({ ...filters, salaryMin: sal.value })}
                className={`px-3 py-1.5 rounded-xl text-xs font-semibold transition-all border ${
                  isSelected
                    ? 'bg-emerald-600 text-white border-emerald-600 shadow-md shadow-emerald-500/20'
                    : 'bg-surface-2/60 text-muted-foreground border-border/60 hover:border-emerald-500/40'
                }`}
              >
                {sal.label}
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
};
