import React from 'react';
import { Layers } from 'lucide-react';

const TECH_STACKS = [
  { name: 'Java & Spring Boot', category: 'Backend' },
  { name: 'React & Next.js', category: 'Frontend' },
  { name: 'Python & FastAPI', category: 'AI & Backend' },
  { name: 'PostgreSQL & SQL', category: 'Database' },
  { name: 'Docker & Kubernetes', category: 'DevOps' },
  { name: 'AWS & Cloud Systems', category: 'Infrastructure' },
  { name: 'TypeScript & Node.js', category: 'Full Stack' },
  { name: 'Go & Distributed Systems', category: 'Systems' },
  { name: 'Redis & Message Queues', category: 'Caching' },
  { name: 'REST & GraphQL APIs', category: 'Architecture' },
];

export const LandingLogoMarquee = () => {
  // Duplicated list for seamless, stutter-free loop
  const marqueeItems = [...TECH_STACKS, ...TECH_STACKS];

  return (
    <section className="py-12 sm:py-16 relative overflow-hidden border-y border-border/50 bg-surface/30">
      <div className="max-w-7xl mx-auto px-4 sm:px-6">
        {/* Title */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-md bg-muted text-foreground text-xs font-mono font-bold uppercase mb-2">
            <Layers className="w-3.5 h-3.5 text-brand-blue" />
            Ecosystem Competencies
          </div>
          <h2 className="text-xl sm:text-2xl lg:text-3xl font-extrabold font-heading text-foreground tracking-tight">
            Production Engineering Stacks Evaluated by HireAI
          </h2>
          <p className="text-xs sm:text-sm text-muted-foreground mt-1 max-w-xl mx-auto font-sans">
            Our deterministic parser and ATS scoring matrix evaluate candidate competencies across industry-standard technologies.
          </p>
        </div>

        {/* Marquee Ticker Track with Gradient Fade Edges */}
        <div className="relative w-full overflow-hidden py-2">
          {/* Left Edge Gradient Fade */}
          <div className="pointer-events-none absolute inset-y-0 left-0 w-20 sm:w-32 bg-gradient-to-r from-background via-background/90 to-transparent z-10" />

          {/* Right Edge Gradient Fade */}
          <div className="pointer-events-none absolute inset-y-0 right-0 w-20 sm:w-32 bg-gradient-to-l from-background via-background/90 to-transparent z-10" />

          {/* Marquee Strip */}
          <div className="flex animate-marquee gap-6 sm:gap-8 items-center will-change-transform">
            {marqueeItems.map((item, index) => (
              <div
                key={`${item.name}-${index}`}
                className="flex items-center gap-3 px-4 py-2 rounded-xl bg-surface border border-border/70 shrink-0 text-foreground shadow-xs hover:border-brand-blue transition-all"
              >
                <span className="w-2 h-2 rounded-full bg-brand-blue shrink-0" />
                <span className="font-bold text-sm font-sans">{item.name}</span>
                <span className="text-[10px] font-mono font-semibold px-2 py-0.5 rounded-md bg-muted text-muted-foreground">
                  {item.category}
                </span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
};
