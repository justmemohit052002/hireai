import React, { useState, useEffect, useMemo, useRef } from 'react';
import { ChevronDown, Phone, Search, Check } from 'lucide-react';
import { cn } from '@/utils';

export const COUNTRIES = [
  { code: 'IN', name: 'India', dialCode: '+91', flag: '🇮🇳', placeholder: '98765 43210' },
  { code: 'US', name: 'United States', dialCode: '+1', flag: '🇺🇸', placeholder: '(555) 000-0000' },
  { code: 'GB', name: 'United Kingdom', dialCode: '+44', flag: '🇬🇧', placeholder: '7911 123456' },
  { code: 'AE', name: 'United Arab Emirates', dialCode: '+971', flag: '🇦🇪', placeholder: '50 123 4567' },
  { code: 'SG', name: 'Singapore', dialCode: '+65', flag: '🇸🇬', placeholder: '8123 4567' },
  { code: 'CA', name: 'Canada', dialCode: '+1', flag: '🇨🇦', placeholder: '(555) 000-0000' },
  { code: 'AU', name: 'Australia', dialCode: '+61', flag: '🇦🇺', placeholder: '412 345 678' },
  { code: 'DE', name: 'Germany', dialCode: '+49', flag: '🇩🇪', placeholder: '151 12345678' },
  { code: 'FR', name: 'France', dialCode: '+33', flag: '🇫🇷', placeholder: '6 12 34 56 78' },
  { code: 'JP', name: 'Japan', dialCode: '+81', flag: '🇯🇵', placeholder: '90 1234 5678' },
  { code: 'SA', name: 'Saudi Arabia', dialCode: '+966', flag: '🇸🇦', placeholder: '50 123 4567' },
  { code: 'NL', name: 'Netherlands', dialCode: '+31', flag: '🇳🇱', placeholder: '6 12345678' },
  { code: 'CH', name: 'Switzerland', dialCode: '+41', flag: '🇨🇭', placeholder: '78 123 45 67' },
  { code: 'BR', name: 'Brazil', dialCode: '+55', flag: '🇧🇷', placeholder: '11 91234-5678' },
  { code: 'ZA', name: 'South Africa', dialCode: '+27', flag: '🇿🇦', placeholder: '82 123 4567' },
  { code: 'NZ', name: 'New Zealand', dialCode: '+64', flag: '🇳🇿', placeholder: '21 123 4567' },
  { code: 'IE', name: 'Ireland', dialCode: '+353', flag: '🇮🇪', placeholder: '85 123 4567' },
  { code: 'MY', name: 'Malaysia', dialCode: '+60', flag: '🇲🇾', placeholder: '12-345 6789' },
  { code: 'PH', name: 'Philippines', dialCode: '+63', flag: '🇵🇭', placeholder: '912 345 6789' },
  { code: 'ID', name: 'Indonesia', dialCode: '+62', flag: '🇮🇩', placeholder: '812-3456-7890' },
];

export function detectUserCountry() {
  try {
    const tz = Intl.DateTimeFormat().resolvedOptions().timeZone || '';
    const lang = (navigator.language || navigator.userLanguage || '').toUpperCase();

    if (tz.includes('Calcutta') || tz.includes('Kolkata') || tz.includes('India') || lang.includes('IN')) return 'IN';
    if (tz.includes('New_York') || tz.includes('Los_Angeles') || tz.includes('Chicago') || tz.includes('Denver') || tz.includes('America/')) return 'US';
    if (tz.includes('London') || lang.includes('GB')) return 'GB';
    if (tz.includes('Dubai') || tz.includes('Abu_Dhabi')) return 'AE';
    if (tz.includes('Singapore') || lang.includes('SG')) return 'SG';
    if (tz.includes('Toronto') || tz.includes('Vancouver') || tz.includes('Montreal')) return 'CA';
    if (tz.includes('Sydney') || tz.includes('Melbourne') || tz.includes('Australia')) return 'AU';
    if (tz.includes('Berlin') || lang.includes('DE')) return 'DE';
    if (tz.includes('Paris') || lang.includes('FR')) return 'FR';
    if (tz.includes('Tokyo') || lang.includes('JP')) return 'JP';
    if (tz.includes('Riyadh')) return 'SA';
    if (tz.includes('Amsterdam') || lang.includes('NL')) return 'NL';
    if (tz.includes('Zurich')) return 'CH';
    if (tz.includes('Sao_Paulo')) return 'BR';
    if (tz.includes('Johannesburg')) return 'ZA';
  } catch {
    // ignore
  }
  return 'IN'; // default to India (+91)
}

export const PhoneInput = React.forwardRef(
  (
    {
      value = '',
      onChange,
      placeholder,
      required = false,
      disabled = false,
      error,
      className,
      ...props
    },
    ref
  ) => {
    // Find initial country from value or detect from user's locale/timezone
    const initialCountryCode = useMemo(() => {
      if (value && typeof value === 'string' && value.startsWith('+')) {
        const found = COUNTRIES.find((c) => value.startsWith(c.dialCode));
        if (found) return found.code;
      }
      return detectUserCountry();
    }, []);

    const [selectedCountryCode, setSelectedCountryCode] = useState(initialCountryCode);
    const [isOpen, setIsOpen] = useState(false);
    const [searchQuery, setSearchQuery] = useState('');
    const dropdownRef = useRef(null);

    const selectedCountry = useMemo(
      () => COUNTRIES.find((c) => c.code === selectedCountryCode) || COUNTRIES[0],
      [selectedCountryCode]
    );

    // Extract national number (remove dial code prefix if present in value)
    const nationalNumber = useMemo(() => {
      if (!value) return '';
      const strVal = String(value);
      if (strVal.startsWith(selectedCountry.dialCode)) {
        return strVal.slice(selectedCountry.dialCode.length).trim();
      }
      return strVal;
    }, [value, selectedCountry.dialCode]);

    // Close dropdown on click outside
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

    const handleNumberChange = (e) => {
      const inputVal = e.target.value;
      const combined = inputVal.trim() ? `${selectedCountry.dialCode} ${inputVal.trim()}` : '';
      if (onChange) {
        onChange(combined, {
          country: selectedCountry.code,
          dialCode: selectedCountry.dialCode,
          nationalNumber: inputVal,
        });
      }
    };

    const handleSelectCountry = (country) => {
      setSelectedCountryCode(country.code);
      setIsOpen(false);
      setSearchQuery('');
      const combined = nationalNumber.trim() ? `${country.dialCode} ${nationalNumber.trim()}` : '';
      if (onChange) {
        onChange(combined, {
          country: country.code,
          dialCode: country.dialCode,
          nationalNumber,
        });
      }
    };

    const filteredCountries = useMemo(() => {
      if (!searchQuery) return COUNTRIES;
      const q = searchQuery.toLowerCase();
      return COUNTRIES.filter(
        (c) =>
          c.name.toLowerCase().includes(q) ||
          c.dialCode.includes(q) ||
          c.code.toLowerCase().includes(q)
      );
    }, [searchQuery]);

    return (
      <div className="w-full">
        <div className="relative flex items-center">
          {/* Country Code Dropdown Trigger */}
          <div className="relative" ref={dropdownRef}>
            <button
              type="button"
              disabled={disabled}
              onClick={() => setIsOpen((prev) => !prev)}
              className={cn(
                'flex h-11 items-center gap-1.5 rounded-l-xl border border-r-0 border-border bg-surface-2 hover:bg-surface-3 px-3 text-xs font-semibold text-foreground transition-colors focus:outline-none shrink-0 cursor-pointer shadow-xs',
                disabled && 'cursor-not-allowed opacity-50',
                error && 'border-red-500'
              )}
              title={`${selectedCountry.name} (${selectedCountry.dialCode})`}
            >
              <span className="text-base leading-none">{selectedCountry.flag}</span>
              <span className="font-mono text-xs">{selectedCountry.dialCode}</span>
              <ChevronDown className={cn('w-3.5 h-3.5 text-muted-foreground transition-transform', isOpen && 'rotate-180')} />
            </button>

            {/* Dropdown Menu */}
            {isOpen && (
              <div className="absolute top-full left-0 mt-1.5 w-64 max-h-64 overflow-hidden rounded-2xl bg-surface-1/95 dark:bg-[#151420]/95 backdrop-blur-xl border border-border/80 shadow-2xl z-50 flex flex-col">
                {/* Search country */}
                <div className="p-2 border-b border-border/50 sticky top-0 bg-surface-1/90 dark:bg-[#151420]/90 backdrop-blur-md">
                  <div className="relative flex items-center">
                    <Search className="w-3.5 h-3.5 text-muted-foreground absolute left-2.5" />
                    <input
                      type="text"
                      value={searchQuery}
                      onChange={(e) => setSearchQuery(e.target.value)}
                      placeholder="Search country or code..."
                      className="w-full pl-8 pr-3 py-1.5 text-xs bg-surface-2 rounded-lg border border-border text-foreground placeholder:text-muted-foreground focus:outline-none"
                      autoFocus
                    />
                  </div>
                </div>

                {/* List */}
                <div className="overflow-y-auto flex-1 p-1">
                  {filteredCountries.map((c) => (
                    <button
                      key={c.code}
                      type="button"
                      onClick={() => handleSelectCountry(c)}
                      className={cn(
                        'flex items-center justify-between w-full px-3 py-2 rounded-xl text-xs text-left transition-colors',
                        c.code === selectedCountryCode
                          ? 'bg-primary/15 text-primary font-bold'
                          : 'text-foreground hover:bg-surface-2'
                      )}
                    >
                      <div className="flex items-center gap-2 truncate">
                        <span className="text-base leading-none">{c.flag}</span>
                        <span className="truncate">{c.name}</span>
                      </div>
                      <div className="flex items-center gap-1.5 shrink-0 ml-2">
                        <span className="font-mono text-[11px] text-muted-foreground">{c.dialCode}</span>
                        {c.code === selectedCountryCode && <Check className="w-3.5 h-3.5 text-primary" />}
                      </div>
                    </button>
                  ))}
                  {filteredCountries.length === 0 && (
                    <div className="p-3 text-center text-xs text-muted-foreground">
                      No country found
                    </div>
                  )}
                </div>
              </div>
            )}
          </div>

          {/* National Phone Number Input */}
          <input
            ref={ref}
            type="tel"
            required={required}
            disabled={disabled}
            value={nationalNumber}
            onChange={handleNumberChange}
            placeholder={placeholder || selectedCountry.placeholder}
            className={cn(
              'flex h-11 w-full rounded-r-xl border border-border bg-surface-2/80 dark:bg-surface-2 text-foreground px-3.5 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#F56681]/40 focus-visible:border-[#F56681] disabled:cursor-not-allowed disabled:opacity-50 transition-all duration-200 shadow-xs font-mono',
              error && 'border-red-500 focus-visible:ring-red-500',
              className
            )}
            {...props}
          />
        </div>

        {error && <p className="mt-1.5 text-xs text-red-500 font-medium">{error}</p>}
      </div>
    );
  }
);

PhoneInput.displayName = 'PhoneInput';
