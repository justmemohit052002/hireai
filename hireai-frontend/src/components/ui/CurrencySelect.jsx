import React, { useState, useEffect, useMemo, useRef } from 'react';
import { ChevronDown, Search, Check, Coins } from 'lucide-react';
import { cn } from '@/utils';

export const CURRENCIES = [
  { code: 'INR', symbol: '₹', name: 'Indian Rupee', flag: '🇮🇳', countries: ['IN'] },
  { code: 'USD', symbol: '$', name: 'US Dollar', flag: '🇺🇸', countries: ['US'] },
  { code: 'EUR', symbol: '€', name: 'Euro', flag: '🇪🇺', countries: ['DE', 'FR', 'NL', 'IT', 'ES', 'IE', 'PT', 'BE', 'AT', 'FI', 'GR'] },
  { code: 'GBP', symbol: '£', name: 'British Pound', flag: '🇬🇧', countries: ['GB'] },
  { code: 'AED', symbol: 'د.إ', name: 'UAE Dirham', flag: '🇦🇪', countries: ['AE'] },
  { code: 'CAD', symbol: 'C$', name: 'Canadian Dollar', flag: '🇨🇦', countries: ['CA'] },
  { code: 'AUD', symbol: 'A$', name: 'Australian Dollar', flag: '🇦🇺', countries: ['AU'] },
  { code: 'SGD', symbol: 'S$', name: 'Singapore Dollar', flag: '🇸🇬', countries: ['SG'] },
  { code: 'SAR', symbol: '﷼', name: 'Saudi Riyal', flag: '🇸🇦', countries: ['SA'] },
  { code: 'JPY', symbol: '¥', name: 'Japanese Yen', flag: '🇯🇵', countries: ['JP'] },
  { code: 'CHF', symbol: 'CHF', name: 'Swiss Franc', flag: '🇨🇭', countries: ['CH'] },
  { code: 'NZD', symbol: 'NZ$', name: 'New Zealand Dollar', flag: '🇳🇿', countries: ['NZ'] },
  { code: 'MYR', symbol: 'RM', name: 'Malaysian Ringgit', flag: '🇲🇾', countries: ['MY'] },
  { code: 'PHP', symbol: '₱', name: 'Philippine Peso', flag: '🇵🇭', countries: ['PH'] },
  { code: 'IDR', symbol: 'Rp', name: 'Indonesian Rupiah', flag: '🇮🇩', countries: ['ID'] },
  { code: 'BRL', symbol: 'R$', name: 'Brazilian Real', flag: '🇧🇷', countries: ['BR'] },
  { code: 'ZAR', symbol: 'R', name: 'South African Rand', flag: '🇿🇦', countries: ['ZA'] },
  { code: 'QAR', symbol: 'QR', name: 'Qatari Riyal', flag: '🇶🇦', countries: ['QA'] },
  { code: 'KWD', symbol: 'KD', name: 'Kuwaiti Dinar', flag: '🇰🇼', countries: ['KW'] },
];

/**
 * Detect user's local currency based on browser timezone and locale
 */
export function detectUserCurrency() {
  try {
    const tz = Intl.DateTimeFormat().resolvedOptions().timeZone || '';
    const lang = (navigator.language || navigator.userLanguage || '').toUpperCase();

    // India
    if (tz.includes('Calcutta') || tz.includes('Kolkata') || tz.includes('India') || lang.includes('IN') || lang.includes('HI')) return 'INR';
    // United States
    if (tz.includes('New_York') || tz.includes('Los_Angeles') || tz.includes('Chicago') || tz.includes('Denver') || tz.includes('America/') || lang.includes('US')) return 'USD';
    // United Kingdom
    if (tz.includes('London') || lang.includes('GB')) return 'GBP';
    // UAE / Middle East
    if (tz.includes('Dubai') || tz.includes('Abu_Dhabi') || lang.includes('AE')) return 'AED';
    // Singapore
    if (tz.includes('Singapore') || lang.includes('SG')) return 'SGD';
    // Canada
    if (tz.includes('Toronto') || tz.includes('Vancouver') || tz.includes('Montreal') || lang.includes('CA')) return 'CAD';
    // Australia
    if (tz.includes('Sydney') || tz.includes('Melbourne') || tz.includes('Australia') || lang.includes('AU')) return 'AUD';
    // Eurozone
    if (
      tz.includes('Berlin') ||
      tz.includes('Paris') ||
      tz.includes('Amsterdam') ||
      tz.includes('Madrid') ||
      tz.includes('Rome') ||
      tz.includes('Europe') ||
      lang.includes('DE') ||
      lang.includes('FR') ||
      lang.includes('ES') ||
      lang.includes('IT') ||
      lang.includes('NL')
    ) {
      return 'EUR';
    }
    // Japan
    if (tz.includes('Tokyo') || lang.includes('JP') || lang.includes('JA')) return 'JPY';
    // Saudi Arabia
    if (tz.includes('Riyadh') || lang.includes('SA')) return 'SAR';
    // Switzerland
    if (tz.includes('Zurich')) return 'CHF';
    // New Zealand
    if (tz.includes('Auckland') || lang.includes('NZ')) return 'NZD';
    // Malaysia
    if (tz.includes('Kuala_Lumpur') || lang.includes('MY')) return 'MYR';
    // Philippines
    if (tz.includes('Manila') || lang.includes('PH')) return 'PHP';
    // Brazil
    if (tz.includes('Sao_Paulo') || lang.includes('BR')) return 'BRL';
    // South Africa
    if (tz.includes('Johannesburg') || lang.includes('ZA')) return 'ZAR';
  } catch {
    // fallback
  }

  return 'INR';
}

export const CurrencySelect = ({
  value,
  onChange,
  disabled = false,
  error,
  className,
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const dropdownRef = useRef(null);

  // Get current active currency item
  const selectedCurrency = useMemo(() => {
    const code = (value || '').toUpperCase();
    return CURRENCIES.find((c) => c.code === code) || CURRENCIES.find((c) => c.code === 'INR') || CURRENCIES[0];
  }, [value]);

  // Close dropdown when clicked outside
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    };
    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
    }
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [isOpen]);

  const filteredCurrencies = useMemo(() => {
    if (!searchQuery.trim()) return CURRENCIES;
    const q = searchQuery.toLowerCase().trim();
    return CURRENCIES.filter(
      (c) =>
        c.code.toLowerCase().includes(q) ||
        c.name.toLowerCase().includes(q) ||
        c.symbol.toLowerCase().includes(q)
    );
  }, [searchQuery]);

  const handleSelect = (code) => {
    if (onChange) {
      onChange(code);
    }
    setIsOpen(false);
    setSearchQuery('');
  };

  return (
    <div className="relative w-full" ref={dropdownRef}>
      <button
        type="button"
        disabled={disabled}
        onClick={() => setIsOpen((prev) => !prev)}
        className={cn(
          'flex h-11 w-full items-center justify-between rounded-xl border border-border bg-surface-2/80 dark:bg-surface-2 text-foreground px-3.5 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-[#F56681]/40 focus:border-[#F56681] disabled:cursor-not-allowed disabled:opacity-50 transition-all duration-200 shadow-xs cursor-pointer',
          error && 'border-red-500 focus:ring-red-500',
          className
        )}
      >
        <div className="flex items-center gap-2">
          <span className="text-base leading-none">{selectedCurrency.flag}</span>
          <span className="font-bold text-xs">{selectedCurrency.code}</span>
          <span className="text-xs text-muted-foreground">({selectedCurrency.symbol})</span>
        </div>
        <ChevronDown className={cn('h-4 w-4 text-muted-foreground transition-transform duration-200', isOpen && 'rotate-180')} />
      </button>

      {isOpen && (
        <div className="absolute top-full left-0 mt-1.5 w-full min-w-[220px] max-h-64 overflow-hidden rounded-2xl bg-surface-1/95 dark:bg-[#151420]/95 backdrop-blur-xl border border-border/80 shadow-2xl z-50 flex flex-col animate-in fade-in-50 zoom-in-95">
          {/* Search Bar */}
          <div className="p-2 border-b border-border/50 sticky top-0 bg-surface-1/90 dark:bg-[#151420]/90 backdrop-blur-md">
            <div className="relative flex items-center">
              <Search className="w-3.5 h-3.5 text-muted-foreground absolute left-2.5" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search currency or country..."
                className="w-full pl-8 pr-3 py-1.5 text-xs bg-surface-2 rounded-lg border border-border text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-1 focus:ring-primary/40"
                autoFocus
              />
            </div>
          </div>

          {/* Currency List */}
          <div className="overflow-y-auto flex-1 p-1">
            {filteredCurrencies.map((c) => {
              const isSelected = c.code === selectedCurrency.code;
              return (
                <button
                  key={c.code}
                  type="button"
                  onClick={() => handleSelect(c.code)}
                  className={cn(
                    'flex items-center justify-between w-full px-3 py-2 rounded-xl text-xs text-left transition-colors cursor-pointer',
                    isSelected
                      ? 'bg-primary/15 text-primary font-bold'
                      : 'text-foreground hover:bg-surface-2'
                  )}
                >
                  <div className="flex items-center gap-2.5 truncate">
                    <span className="text-base leading-none">{c.flag}</span>
                    <span className="font-bold">{c.code}</span>
                    <span className="text-muted-foreground text-[11px] truncate">({c.name})</span>
                  </div>
                  <div className="flex items-center gap-1.5 shrink-0 ml-2">
                    <span className="font-mono text-xs font-semibold text-foreground/80">{c.symbol}</span>
                    {isSelected && <Check className="w-3.5 h-3.5 text-primary" />}
                  </div>
                </button>
              );
            })}
            {filteredCurrencies.length === 0 && (
              <div className="p-3 text-center text-xs text-muted-foreground">
                No currencies found
              </div>
            )}
          </div>
        </div>
      )}

      {error && <p className="mt-1.5 text-xs text-red-500 font-medium">{error}</p>}
    </div>
  );
};
