import React from 'react';
import { Card } from '@/components/ui/Card';
import { Sparkles } from 'lucide-react';

export const ChartPlaceholder = () => {
  const bars = [40, 65, 45, 80, 95, 70, 85, 60, 90, 100, 75, 85];
  const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

  return (
    <Card surface-nested className="p-6 space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h3 className="text-lg font-bold font-heading text-foreground">Candidate Pipeline Dynamics</h3>
          <p className="text-xs text-muted-foreground">AI Sourced vs Applied Candidates over 2026</p>
        </div>
        <div className="flex items-center gap-1.5 px-3 py-1 rounded-full bg-blue-500/10 text-blue-500 border border-blue-500/20 text-xs font-bold">
          <Sparkles className="w-3.5 h-3.5" />
          <span>Real-time Analytics</span>
        </div>
      </div>

      {/* Bar Chart Visualization */}
      <div className="h-48 flex items-end gap-2 md:gap-4 pt-6 border-b border-border/40 pb-2">
        {bars.map((height, idx) => (
          <div key={idx} className="flex-1 flex flex-col items-center gap-2 group h-full justify-end">
            <div
              style={{ height: `${height}%` }}
              className="w-full rounded-t-lg bg-gradient-to-t from-blue-600 via-indigo-500 to-purple-500 opacity-80 group-hover:opacity-100 transition-all duration-300 relative"
            >
              {/* Tooltip on hover */}
              <div className="opacity-0 group-hover:opacity-100 absolute -top-8 left-1/2 -translate-x-1/2 bg-foreground text-background text-[10px] font-mono font-bold px-1.5 py-0.5 rounded shadow transition-opacity pointer-events-none whitespace-nowrap">
                {height * 12} apps
              </div>
            </div>
            <span className="text-[10px] text-muted-foreground font-mono">{months[idx]}</span>
          </div>
        ))}
      </div>
    </Card>
  );
};
