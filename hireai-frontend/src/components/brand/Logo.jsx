import React from 'react';
import { cn } from '@/utils';

const SIZES = {
  sm: {
    iconClass: 'w-[28px] h-[28px]',
    textClass: 'text-lg',
    gapClass: 'gap-2',
  },
  md: {
    iconClass: 'w-[36px] h-[36px]',
    textClass: 'text-xl',
    gapClass: 'gap-2.5',
  },
  lg: {
    iconClass: 'w-[48px] h-[48px]',
    textClass: 'text-2xl',
    gapClass: 'gap-3',
  },
};

/**
 * Reusable HireAI Brand Logo Component
 *
 * @param {'full' | 'icon'} variant - Visual display variant: "full" (icon + wordmark + optional badge) or "icon" (icon only)
 * @param {'sm' | 'md' | 'lg'} size - Icon size: sm (28px), md (36px), lg (48px) with proportional wordmark
 * @param {boolean} showPro - Whether to display the PRO badge when variant="full" (default: true)
 * @param {string} className - Additional CSS classes applied to root container
 */
export const Logo = ({
  variant = 'full',
  size = 'md',
  showPro = true,
  iconOnly = false,
  className = '',
}) => {
  const sizeConfig = SIZES[size] || SIZES.md;
  const isFull = variant === 'full' && !iconOnly;

  return (
    <div
      className={cn('inline-flex items-center select-none shrink-0 whitespace-nowrap', sizeConfig.gapClass, className)}
      aria-label="HireAI"
      role={!isFull ? 'img' : undefined}
    >
      {/* Icon (inline SVG viewBox 0 0 64 64) */}
      <svg
        viewBox="0 0 64 64"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        className={cn('shrink-0', sizeConfig.iconClass)}
        aria-hidden={isFull ? 'true' : undefined}
      >
        {/* Rounded square with 1px stroke in dark mode */}
        <rect
          x="0"
          y="0"
          width="64"
          height="64"
          rx="16"
          className="fill-brand-navy stroke-transparent dark:stroke-brand-blue/50"
          strokeWidth="1"
        />

        {/* Two vertical bars, fill white */}
        <rect x="17" y="15" width="8" height="34" rx="3" fill="white" />
        <rect x="39" y="15" width="8" height="34" rx="3" fill="white" />

        {/* Crossbar joining them, fill Lemon #F1FF62 (no rounding) */}
        <rect x="25" y="28" width="14" height="8" className="fill-brand-accent" />

        {/* Spark dot, fill Lemon #F1FF62 */}
        <circle cx="53" cy="10" r="4" className="fill-brand-accent" />
      </svg>

      {/* Wordmark and PRO Badge */}
      {isFull && (
        <div className="flex items-center gap-2 font-heading leading-none shrink-0 whitespace-nowrap">
          <span className={cn('font-medium tracking-tight shrink-0 whitespace-nowrap', sizeConfig.textClass)}>
            <span className="text-brand-dark dark:text-[#F4F5FA]">Hire</span>
            <span className="text-brand-blue dark:text-brand-blue-light">AI</span>
          </span>

          {showPro && (
            <span className="bg-brand-accent text-brand-dark text-xs font-bold tracking-wider px-2 py-0.5 rounded-md select-none uppercase shrink-0 whitespace-nowrap">
              PRO
            </span>
          )}
        </div>
      )}
    </div>
  );
};

export default Logo;
