import React from 'react';

const COMPANIES = [
  {
    name: 'Postman',
    icon: (
      <svg className="w-5 h-5 shrink-0" viewBox="0 0 24 24" fill="#FF6C37">
        <circle cx="12" cy="12" r="10" />
        <path d="M12 4a8 8 0 100 16 8 8 0 000-16zm-1.5 5.5l5 2.5-5 2.5v-5z" fill="white" />
      </svg>
    ),
    label: 'POSTMAN',
    fontClass: 'font-extrabold tracking-widest text-[#FF6C37]',
  },
  {
    name: 'Klaviyo',
    isBoxLogo: true,
    boxBg: 'bg-[#E35238]',
    boxText: 'klaviyo',
    label: 'klaviyo',
  },
  {
    name: 'Braze',
    isScript: true,
    label: 'braze',
    fontClass: 'font-serif italic font-bold tracking-tight text-xl',
  },
  {
    name: 'Zipline',
    label: 'zipline',
    fontClass: 'font-black tracking-tight text-xl',
  },
  {
    name: 'Applied Intuition',
    icon: (
      <svg className="w-4 h-4 text-current shrink-0" viewBox="0 0 24 24" fill="currentColor">
        <path d="M12 2L3 19h5l4-8 4 8h5L12 2z" />
      </svg>
    ),
    label: 'Applied Intuition',
    fontClass: 'font-bold tracking-tight text-base',
  },
  {
    name: 'Astranis',
    icon: (
      <svg className="w-4 h-4 text-current shrink-0" viewBox="0 0 24 24" fill="currentColor">
        <path d="M12 3L4 21h4l4-10 4 10h4L12 3z" />
      </svg>
    ),
    label: 'ASTRANIS',
    fontClass: 'font-black tracking-widest text-base font-mono',
  },
  {
    name: 'IonQ',
    icon: (
      <div className="w-4 h-4 rounded-sm bg-gradient-to-tr from-[#FFAA00] to-[#00A3E0] flex items-center justify-center text-[9px] font-black text-white shrink-0">
        Q
      </div>
    ),
    label: 'IONQ',
    fontClass: 'font-black tracking-wider text-lg font-heading',
  },
  {
    name: 'Oklo',
    icon: (
      <svg className="w-4 h-4 text-current shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
        <circle cx="12" cy="12" r="8" />
        <path d="M12 4v4m0 8v4M4 12h4m8 0h4" />
      </svg>
    ),
    label: 'Oklo',
    fontClass: 'font-bold tracking-tight text-lg',
  },
  {
    name: 'Peloton',
    icon: (
      <svg className="w-4 h-4 text-current shrink-0" viewBox="0 0 24 24" fill="currentColor">
        <circle cx="12" cy="12" r="9" />
        <path d="M12 7a5 5 0 00-5 5h3a2 2 0 012-2V7z" fill="white" />
      </svg>
    ),
    label: 'PELOTON',
    fontClass: 'font-black tracking-widest text-base font-heading',
  },
  {
    name: 'Stripe',
    label: 'stripe',
    fontClass: 'font-black tracking-tight text-xl',
  },
  {
    name: 'OpenAI',
    icon: (
      <svg className="w-4 h-4 text-current shrink-0" viewBox="0 0 24 24" fill="currentColor">
        <path d="M22.28 9.37a7.99 7.99 0 00-.73-6.02 8.08 8.08 0 00-7.39-4.22 8 8 0 00-2.1.28 8.06 8.06 0 00-5.74 3.12 8 8 0 00-.91 6.57 8.08 8.08 0 00-3.9 4.3 8.06 8.06 0 00.91 8.27 8.04 8.04 0 006.02.73 8.08 8.08 0 007.4 4.22 8.03 8.03 0 002.09-.28 8.06 8.06 0 005.74-3.12 8 8 0 00.92-6.57 8.07 8.07 0 003.89-4.3 8.06 8.06 0 00-.91-8.27z" />
      </svg>
    ),
    label: 'OpenAI',
    fontClass: 'font-bold tracking-tight text-base',
  },
];

export const LandingLogoMarquee = () => {
  // Duplicated list for seamless, stutter-free infinite loop
  const marqueeItems = [...COMPANIES, ...COMPANIES];

  return (
    <section className="py-12 sm:py-16 relative overflow-hidden border-y border-border/50 bg-surface/30">
      <div className="max-w-7xl mx-auto px-4 sm:px-6">
        {/* Title Matching the Provided Reference */}
        <div className="text-center mb-8">
          <h2 className="text-xl sm:text-2xl lg:text-3xl font-normal font-sans text-foreground/90 tracking-tight">
            The teams building what's next{' '}
            <span className="italic font-serif font-semibold text-foreground">
              hire on HireAI.
            </span>
          </h2>
        </div>

        {/* Marquee Ticker Track with Gradient Fade Edges */}
        <div className="relative w-full overflow-hidden py-2">
          {/* Left Edge Gradient Fade */}
          <div className="pointer-events-none absolute inset-y-0 left-0 w-20 sm:w-32 bg-gradient-to-r from-background via-background/90 to-transparent z-10" />

          {/* Right Edge Gradient Fade */}
          <div className="pointer-events-none absolute inset-y-0 right-0 w-20 sm:w-32 bg-gradient-to-l from-background via-background/90 to-transparent z-10" />

          {/* Marquee Strip with Hardware Acceleration */}
          <div className="flex animate-marquee gap-10 sm:gap-16 items-center will-change-transform">
            {marqueeItems.map((company, index) => (
              <div
                key={`${company.name}-${index}`}
                className="flex items-center gap-2.5 px-3 py-1.5 transition-all duration-200 hover:scale-105 cursor-pointer shrink-0 text-foreground/75 hover:text-foreground grayscale hover:grayscale-0 opacity-80 hover:opacity-100"
                title={`${company.name} hires engineers on HireAI`}
              >
                {/* Specific Box Logo Render (like Klaviyo) */}
                {company.isBoxLogo ? (
                  <div className="bg-[#E35238] text-white px-2.5 py-0.5 rounded-sm font-serif font-bold tracking-tight text-base shadow-2xs">
                    {company.boxText}
                  </div>
                ) : (
                  <>
                    {company.icon}
                    <span className={company.fontClass || 'font-bold text-base'}>
                      {company.label}
                    </span>
                  </>
                )}
              </div>
            ))}
          </div>
        </div>

        {/* Bottom Subtitle Tagline Matching the Reference Image */}
        <div className="text-center mt-7">
          <p className="text-[11px] sm:text-xs font-mono font-medium text-muted-foreground tracking-wider uppercase">
            <span className="text-red-500 dark:text-red-400 font-bold">+ 27,000</span>{' '}
            MORE TECH TEAMS HIRING ON HIREAI TODAY
          </p>
        </div>
      </div>
    </section>
  );
};
