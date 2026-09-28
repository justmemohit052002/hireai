import React from 'react';
import { Sparkles, Users, UserCheck, Briefcase } from 'lucide-react';

const COMPANIES = [
  {
    name: 'Postman',
    color: '#FF6C37',
    icon: (
      <svg className="w-6 h-6" viewBox="0 0 24 24" fill="#FF6C37">
        <circle cx="12" cy="12" r="10" />
        <path d="M12 4a8 8 0 100 16 8 8 0 000-16zm-1.5 5.5l5 2.5-5 2.5v-5z" fill="white" />
      </svg>
    ),
    label: 'POSTMAN',
    fontClass: 'font-extrabold tracking-widest text-[#FF6C37]',
    rolesCount: '48 Roles',
  },
  {
    name: 'Klaviyo',
    color: '#F46036',
    icon: null,
    isBoxLogo: true,
    boxBg: 'bg-[#E35238]',
    boxText: 'klaviyo',
    label: 'klaviyo',
    rolesCount: '32 Roles',
  },
  {
    name: 'Braze',
    color: '#111111',
    icon: null,
    isScript: true,
    label: 'braze',
    fontClass: 'font-serif italic font-bold tracking-tight text-2xl',
    rolesCount: '27 Roles',
  },
  {
    name: 'Zipline',
    color: '#000000',
    icon: null,
    label: 'zipline',
    fontClass: 'font-black tracking-tight text-2xl',
    rolesCount: '19 Roles',
  },
  {
    name: 'Applied Intuition',
    color: '#000000',
    icon: (
      <svg className="w-5 h-5 text-current" viewBox="0 0 24 24" fill="currentColor">
        <path d="M12 2L3 19h5l4-8 4 8h5L12 2z" />
      </svg>
    ),
    label: 'Applied Intuition',
    fontClass: 'font-bold tracking-tight text-lg',
    rolesCount: '54 Roles',
  },
  {
    name: 'Astranis',
    color: '#000000',
    icon: (
      <svg className="w-5 h-5 text-current" viewBox="0 0 24 24" fill="currentColor">
        <path d="M12 3L4 21h4l4-10 4 10h4L12 3z" />
      </svg>
    ),
    label: 'ASTRANIS',
    fontClass: 'font-black tracking-widest text-lg font-mono',
    rolesCount: '21 Roles',
  },
  {
    name: 'IonQ',
    color: '#00A3E0',
    icon: (
      <div className="w-5 h-5 rounded-md bg-gradient-to-tr from-[#FFAA00] to-[#00A3E0] flex items-center justify-center text-[10px] font-black text-white shadow-xs">
        Q
      </div>
    ),
    label: 'IONQ',
    fontClass: 'font-black tracking-wider text-xl font-heading',
    rolesCount: '15 Roles',
  },
  {
    name: 'Oklo',
    color: '#1A1A1A',
    icon: (
      <svg className="w-5 h-5 text-current" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
        <circle cx="12" cy="12" r="8" />
        <path d="M12 4v4m0 8v4M4 12h4m8 0h4" />
      </svg>
    ),
    label: 'Oklo',
    fontClass: 'font-bold tracking-tight text-xl',
    rolesCount: '23 Roles',
  },
  {
    name: 'Peloton',
    color: '#DF1B24',
    icon: (
      <svg className="w-5 h-5 text-current" viewBox="0 0 24 24" fill="currentColor">
        <circle cx="12" cy="12" r="9" />
        <path d="M12 7a5 5 0 00-5 5h3a2 2 0 012-2V7z" fill="white" />
      </svg>
    ),
    label: 'PELOTON',
    fontClass: 'font-black tracking-widest text-lg font-heading',
    rolesCount: '36 Roles',
  },
  {
    name: 'Stripe',
    color: '#635BFF',
    icon: null,
    label: 'stripe',
    fontClass: 'font-black tracking-tight text-2xl text-[#635BFF]',
    rolesCount: '62 Roles',
  },
  {
    name: 'OpenAI',
    color: '#10A37F',
    icon: (
      <svg className="w-5 h-5 text-[#10A37F]" viewBox="0 0 24 24" fill="currentColor">
        <path d="M22.28 9.37a7.99 7.99 0 00-.73-6.02 8.08 8.08 0 00-7.39-4.22 8 8 0 00-2.1.28 8.06 8.06 0 00-5.74 3.12 8 8 0 00-.91 6.57 8.08 8.08 0 00-3.9 4.3 8.06 8.06 0 00.91 8.27 8.04 8.04 0 006.02.73 8.08 8.08 0 007.4 4.22 8.03 8.03 0 002.09-.28 8.06 8.06 0 005.74-3.12 8 8 0 00.92-6.57 8.07 8.07 0 003.89-4.3 8.06 8.06 0 00-.91-8.27z" />
      </svg>
    ),
    label: 'OpenAI',
    fontClass: 'font-bold tracking-tight text-lg text-foreground',
    rolesCount: '41 Roles',
  },
  {
    name: 'Datadog',
    color: '#632CA6',
    icon: (
      <div className="w-5 h-5 rounded-full bg-[#632CA6] flex items-center justify-center text-white text-[10px] font-bold">
        DD
      </div>
    ),
    label: 'DATADOG',
    fontClass: 'font-black tracking-wider text-lg text-[#632CA6]',
    rolesCount: '38 Roles',
  },
];

export const LandingLogoMarquee = () => {
  // Duplicate list to achieve a continuous, stutter-free infinite marquee loop
  const marqueeItems = [...COMPANIES, ...COMPANIES];

  return (
    <section className="py-12 md:py-16 relative overflow-hidden">
      <div className="max-w-7xl mx-auto px-4 sm:px-6">
        {/* Title Matching the Provided Reference */}
        <div className="text-center mb-8 sm:mb-10">
          <h2 className="text-2xl sm:text-3xl lg:text-4xl font-semibold font-heading text-foreground tracking-tight">
            The teams building what's next{' '}
            <span className="italic font-serif font-bold text-brand-blue dark:text-brand-accent">
              hire on HireAI.
            </span>
          </h2>
          <p className="mt-2 text-xs sm:text-sm text-muted-foreground font-sans">
            From high-growth Series A disruptors to premier Fortune 500 engineering organizations.
          </p>
        </div>

        {/* Marquee Ticker Track with Gradient Fade Edges */}
        <div className="relative w-full overflow-hidden py-3">
          {/* Left Edge Gradient Fade */}
          <div className="pointer-events-none absolute inset-y-0 left-0 w-16 sm:w-28 bg-gradient-to-r from-background via-background/80 to-transparent z-10" />

          {/* Right Edge Gradient Fade */}
          <div className="pointer-events-none absolute inset-y-0 right-0 w-16 sm:w-28 bg-gradient-to-l from-background via-background/80 to-transparent z-10" />

          {/* Marquee Strip */}
          <div className="flex animate-marquee gap-8 sm:gap-14 items-center">
            {marqueeItems.map((company, index) => (
              <div
                key={`${company.name}-${index}`}
                className="group flex items-center gap-2.5 px-4 py-2 rounded-2xl transition-all duration-200 hover:scale-105 hover:bg-surface/80 hover:shadow-md cursor-pointer shrink-0 border border-transparent hover:border-border/60"
              >
                {/* Specific Box Logo Render (like Klaviyo) */}
                {company.isBoxLogo ? (
                  <div className="bg-[#E35238] text-white px-3 py-1 rounded-sm font-serif font-bold tracking-tight text-lg shadow-xs flex items-center gap-1.5">
                    <span className="w-1.5 h-1.5 rounded-full bg-white/80 animate-ping inline-block" />
                    <span>{company.boxText}</span>
                  </div>
                ) : (
                  <>
                    {company.icon && (
                      <span className="shrink-0 transition-transform duration-200 group-hover:scale-110">
                        {company.icon}
                      </span>
                    )}
                    <span
                      className={`${company.fontClass || 'font-bold text-lg'} text-foreground/80 dark:text-foreground/90 transition-colors group-hover:text-foreground`}
                    >
                      {company.label}
                    </span>
                  </>
                )}

                {/* Subtle Candidate Hiring Badge */}
                <span className="hidden group-hover:inline-flex items-center gap-1 text-[10px] font-mono font-semibold px-2 py-0.5 rounded-full bg-brand-blue/15 text-brand-blue dark:text-brand-accent border border-brand-blue/25 transition-all">
                  <UserCheck className="w-2.5 h-2.5" />
                  {company.rolesCount}
                </span>
              </div>
            ))}
          </div>
        </div>

        {/* Bottom Subtitle Tagline Matching the Reference */}
        <div className="text-center mt-8">
          <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-muted/40 dark:bg-muted/20 border border-border/60 backdrop-blur-xs text-[11px] sm:text-xs font-mono font-semibold text-muted-foreground tracking-wider uppercase">
            <span className="text-[#FF5252] dark:text-[#FF7070] font-bold text-sm">+ 27,000</span>
            <span>MORE TECH TEAMS HIRING CANDIDATES ON HIREAI TODAY</span>
          </div>
        </div>
      </div>
    </section>
  );
};
