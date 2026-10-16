import React, { useState } from 'react';

/**
 * Responsive, pure SVG Time-Series Line and Area Chart for Platform Metrics.
 * Uses official brand blues (#004499, #0192C6) and slate neutrals.
 * Zero external charting dependencies to ensure 100% stability with React 19.
 */
export const PlatformTimeSeriesChart = ({
  data = [],
  series = [
    { key: 'signups', label: 'Signups', color: '#0192C6', secondaryColor: '#004499' },
    { key: 'activeUsers', label: 'Active Users', color: '#10B981', secondaryColor: '#059669' },
  ],
  title,
  subtitle,
  height = 200,
}) => {
  const [hoveredIndex, setHoveredIndex] = useState(null);

  if (!data || data.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center p-8 text-center bg-surface-2/40 rounded-2xl border border-white/10" style={{ height }}>
        <p className="text-xs text-muted-foreground font-medium">No time-series telemetry recorded for this scope.</p>
      </div>
    );
  }

  const padding = { top: 20, right: 20, bottom: 30, left: 40 };
  const width = 600; // SVG viewBox coordinate space
  const chartWidth = width - padding.left - padding.right;
  const chartHeight = height - padding.top - padding.bottom;

  // Compute maximum value across all plotted series
  let maxValue = 1;
  data.forEach((d) => {
    series.forEach((s) => {
      const val = Number(d[s.key] || 0);
      if (val > maxValue) maxValue = val;
    });
  });
  // Add 10% headroom
  maxValue = Math.ceil(maxValue * 1.15) || 10;

  const getX = (index) => {
    if (data.length <= 1) return padding.left + chartWidth / 2;
    return padding.left + (index / (data.length - 1)) * chartWidth;
  };

  const getY = (val) => {
    return padding.top + chartHeight - (val / maxValue) * chartHeight;
  };

  const yTicks = [0, Math.round(maxValue / 2), maxValue];

  return (
    <div className="space-y-3">
      {(title || subtitle) && (
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
          <div>
            {title && <h4 className="text-xs font-bold uppercase tracking-wider text-foreground">{title}</h4>}
            {subtitle && <p className="text-[11px] text-muted-foreground">{subtitle}</p>}
          </div>
          {/* Legend */}
          <div className="flex items-center gap-4 text-[11px]">
            {series.map((s) => (
              <div key={s.key} className="flex items-center gap-1.5 font-medium text-muted-foreground">
                <span className="w-2.5 h-2.5 rounded-sm" style={{ backgroundColor: s.color }} />
                <span>{s.label}</span>
              </div>
            ))}
          </div>
        </div>
      )}

      <div className="relative w-full overflow-hidden">
        <svg
          viewBox={`0 0 ${width} ${height}`}
          className="w-full h-auto overflow-visible select-none"
          preserveAspectRatio="none"
        >
          <defs>
            {series.map((s) => (
              <linearGradient key={`grad-${s.key}`} id={`grad-${s.key}`} x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor={s.color} stopOpacity="0.25" />
                <stop offset="100%" stopColor={s.color} stopOpacity="0.0" />
              </linearGradient>
            ))}
          </defs>

          {/* Grid lines */}
          {yTicks.map((tickVal, i) => {
            const y = getY(tickVal);
            return (
              <g key={i}>
                <line
                  x1={padding.left}
                  y1={y}
                  x2={width - padding.right}
                  y2={y}
                  stroke="currentColor"
                  className="text-border/40"
                  strokeDasharray="3 3"
                />
                <text
                  x={padding.left - 8}
                  y={y + 3}
                  textAnchor="end"
                  className="text-[9px] fill-muted-foreground font-mono"
                >
                  {tickVal}
                </text>
              </g>
            );
          })}

          {/* Render Area & Path for each series */}
          {series.map((s) => {
            if (data.length <= 1) return null;

            const points = data.map((d, idx) => ({
              x: getX(idx),
              y: getY(Number(d[s.key] || 0)),
            }));

            const linePath = points.reduce((acc, pt, idx) => {
              return `${acc} ${idx === 0 ? 'M' : 'L'} ${pt.x} ${pt.y}`;
            }, '');

            const areaPath = `${linePath} L ${points[points.length - 1].x} ${padding.top + chartHeight} L ${points[0].x} ${padding.top + chartHeight} Z`;

            return (
              <g key={s.key}>
                <path d={areaPath} fill={`url(#grad-${s.key})`} />
                <path
                  d={linePath}
                  fill="none"
                  stroke={s.color}
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
                {/* Data Points */}
                {points.map((pt, idx) => (
                  <circle
                    key={idx}
                    cx={pt.x}
                    cy={pt.y}
                    r={hoveredIndex === idx ? 4 : 2.5}
                    fill={s.color}
                    className="transition-all"
                  />
                ))}
              </g>
            );
          })}

          {/* X Axis Date labels (sample up to 6 labels) */}
          {data.map((d, idx) => {
            const step = Math.max(1, Math.floor(data.length / 6));
            if (idx % step !== 0 && idx !== data.length - 1) return null;
            const x = getX(idx);
            // Format date e.g. "10-14"
            const label = d.date ? d.date.slice(5) : '';
            return (
              <text
                key={idx}
                x={x}
                y={height - 8}
                textAnchor="middle"
                className="text-[9px] fill-muted-foreground font-mono"
              >
                {label}
              </text>
            );
          })}

          {/* Transparent interactive columns for hover */}
          {data.map((_, idx) => {
            const x = getX(idx);
            const colWidth = chartWidth / data.length;
            return (
              <rect
                key={idx}
                x={x - colWidth / 2}
                y={padding.top}
                width={colWidth}
                height={chartHeight}
                fill="transparent"
                className="cursor-pointer"
                onMouseEnter={() => setHoveredIndex(idx)}
                onMouseLeave={() => setHoveredIndex(null)}
              />
            );
          })}

          {/* Active Hover Line */}
          {hoveredIndex !== null && (
            <line
              x1={getX(hoveredIndex)}
              y1={padding.top}
              x2={getX(hoveredIndex)}
              y2={padding.top + chartHeight}
              stroke="#0192C6"
              strokeWidth="1.5"
              strokeDasharray="2 2"
            />
          )}
        </svg>

        {/* Floating Tooltip */}
        {hoveredIndex !== null && data[hoveredIndex] && (
          <div
            className="absolute z-20 pointer-events-none transform -translate-x-1/2 -translate-y-full bg-surface-1 border border-border shadow-xl rounded-xl p-2.5 text-xs space-y-1"
            style={{
              left: `${(getX(hoveredIndex) / width) * 100}%`,
              top: `${getY(Number(data[hoveredIndex][series[0].key] || 0))}px`,
            }}
          >
            <div className="font-bold text-foreground text-[11px] font-mono border-b border-border/40 pb-1">
              {data[hoveredIndex].date}
            </div>
            {series.map((s) => (
              <div key={s.key} className="flex items-center justify-between gap-3 text-[11px]">
                <span className="flex items-center gap-1.5 font-medium text-muted-foreground">
                  <span className="w-2 h-2 rounded-full" style={{ backgroundColor: s.color }} />
                  {s.label}:
                </span>
                <span className="font-bold font-mono text-foreground">
                  {data[hoveredIndex][s.key] ?? 0}
                </span>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};
