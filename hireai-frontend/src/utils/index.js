import { clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';

// ─────────────────────────────────────────
// Tailwind class merge utility
// ─────────────────────────────────────────
export function cn(...inputs) {
  return twMerge(clsx(inputs));
}

// ─────────────────────────────────────────
// Date & Time Formatters
// ─────────────────────────────────────────
export function formatRelativeTime(date) {
  if (!date) return 'just now';
  const d = typeof date === 'string' ? new Date(date) : date;
  if (isNaN(d?.getTime())) return 'just now';
  const now = new Date();
  const diffMs = now.getTime() - d.getTime();
  const diffSec = Math.floor(diffMs / 1000);
  const diffMin = Math.floor(diffSec / 60);
  const diffHr = Math.floor(diffMin / 60);
  const diffDay = Math.floor(diffHr / 24);

  if (diffSec < 60) return 'just now';
  if (diffMin < 60) return `${diffMin}m ago`;
  if (diffHr < 24) return `${diffHr}h ago`;
  if (diffDay < 7) return `${diffDay}d ago`;
  return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
}

export function formatDate(date) {
  if (!date) return '';
  const d = typeof date === 'string' ? new Date(date) : date;
  if (isNaN(d?.getTime())) return '';
  return d.toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' });
}

export function formatShortDate(date) {
  if (!date) return '';
  const d = typeof date === 'string' ? new Date(date) : date;
  if (isNaN(d?.getTime())) return '';
  return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
}

// ─────────────────────────────────────────
// Salary Formatter
// ─────────────────────────────────────────
export function formatSalary(salary) {
  if (!salary) return 'Competitive';

  const min = Number(salary.min ?? salary.salaryMin ?? 0);
  const max = Number(salary.max ?? salary.salaryMax ?? 0);
  const rawCurrency = salary.currency || 'INR';
  const currency = rawCurrency.toUpperCase();
  const rawPeriod = salary.period || 'year';
  const periodLabel = rawPeriod === 'year' || rawPeriod === 'yr' ? 'yr' : rawPeriod === 'month' || rawPeriod === 'mo' ? 'mo' : rawPeriod;

  if (min === 0 && max === 0) {
    return 'Competitive';
  }

  const symbols = {
    USD: '$',
    EUR: '€',
    GBP: '£',
    CAD: 'CA$',
    AUD: 'AU$',
    INR: '₹',
    AED: 'AED ',
    SAR: 'SAR ',
    SGD: 'S$',
    JPY: '¥',
    CHF: 'CHF ',
    NZD: 'NZ$',
    MYR: 'RM ',
    PHP: '₱',
    IDR: 'Rp ',
    BRL: 'R$',
    ZAR: 'R ',
    QAR: 'QAR ',
    KWD: 'KWD ',
  };
  const symbol = symbols[currency] || (currency ? `${currency} ` : '₹');

  const fmt = (n) => {
    if (!n || isNaN(n)) return '0';
    if (currency === 'INR') {
      if (n >= 10000000) return `${(n / 10000000).toFixed(1).replace(/\.0$/, '')} Cr`;
      if (n >= 100000) return `${(n / 100000).toFixed(1).replace(/\.0$/, '')} LPA`;
      if (n >= 1000) return `${(n / 1000).toFixed(0)}k`;
      return `${n}`;
    } else {
      if (n >= 1000000) return `${(n / 1000000).toFixed(1).replace(/\.0$/, '')}M`;
      if (n >= 1000) return `${(n / 1000).toFixed(0)}k`;
      return `${n}`;
    }
  };

  if (min > 0 && max > 0) {
    if (currency === 'INR' && (min >= 100000 || max >= 100000)) {
      return `${symbol}${fmt(min)} – ${symbol}${fmt(max)}`;
    }
    return `${symbol}${fmt(min)} – ${symbol}${fmt(max)} / ${periodLabel}`;
  }
  if (max > 0) return `Up to ${symbol}${fmt(max)} / ${periodLabel}`;
  if (min > 0) return `From ${symbol}${fmt(min)} / ${periodLabel}`;
  return 'Competitive';
}

// ─────────────────────────────────────────
// Label Helpers
// ─────────────────────────────────────────
export function getJobTypeLabel(type) {
  if (!type) return 'Full-time';
  const map = {
    'full-time': 'Full-time',
    'part-time': 'Part-time',
    contract: 'Contract',
    internship: 'Internship',
    remote: 'Remote',
  };
  return map[type] || type;
}

export function getExperienceLevelLabel(level) {
  if (!level) return 'Mid Level';
  const map = {
    entry: 'Entry Level',
    mid: 'Mid Level',
    senior: 'Senior',
    lead: 'Lead',
    executive: 'Executive',
  };
  return map[level] || level;
}

export function getApplicationStatusLabel(status) {
  if (!status) return 'Applied';
  const map = {
    applied: 'Applied',
    screening: 'Screening',
    interview: 'Interview',
    technical: 'Technical',
    offer: 'Offer',
    rejected: 'Rejected',
    withdrawn: 'Withdrawn',
  };
  return map[status] || status;
}

export function getApplicationStatusColor(status) {
  const map = {
    applied: 'bg-blue-500/10 text-blue-500 border-blue-500/20',
    screening: 'bg-amber-500/10 text-amber-500 border-amber-500/20',
    interview: 'bg-violet-500/10 text-violet-500 border-violet-500/20',
    technical: 'bg-cyan-500/10 text-cyan-500 border-cyan-500/20',
    offer: 'bg-emerald-500/10 text-emerald-500 border-emerald-500/20',
    rejected: 'bg-red-500/10 text-red-500 border-red-500/20',
    withdrawn: 'bg-slate-500/10 text-muted-foreground border-slate-500/20',
  };
  return map[status] || 'bg-slate-500/10 text-muted-foreground border-slate-500/20';
}

export function getBadgeLabel(badge) {
  const map = {
    'top-performer': 'Top Performer',
    'fast-responder': 'Fast Responder',
    'highly-skilled': 'Highly Skilled',
    'rising-star': 'Rising Star',
    verified: 'Verified',
  };
  return map[badge] || badge;
}

// ─────────────────────────────────────────
// Number Formatter
// ─────────────────────────────────────────
export function formatNumber(n) {
  if (!n || isNaN(n)) return '0';
  if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`;
  if (n >= 1_000) return `${(n / 1_000).toFixed(1)}k`;
  return `${n}`;
}

// ─────────────────────────────────────────
// String helpers
// ─────────────────────────────────────────
export function truncate(text, maxLength = 50) {
  if (!text || typeof text !== 'string') return '';
  if (text.length <= maxLength) return text;
  return `${text.slice(0, maxLength).trimEnd()}…`;
}

export function getInitials(name) {
  if (!name || typeof name !== 'string') return 'U';
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return 'U';
  return parts
    .slice(0, 2)
    .map(part => part[0]?.toUpperCase() ?? '')
    .join('');
}

export function generateId() {
  return Math.random().toString(36).slice(2, 9);
}
