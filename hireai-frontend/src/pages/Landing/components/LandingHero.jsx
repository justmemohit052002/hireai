import React from 'react';
import { Link } from 'react-router-dom';
import { 
  UserCheck, 
  UserPlus, 
  Briefcase, 
  Sparkles, 
  BadgeCheck, 
  Clock, 
  ArrowRight,
  Search,
  CheckCircle2,
  Building2,
  Code2
} from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { ROUTES } from '@/constants';

export const LandingHero = () => {
  return (
    <section className="relative pt-6 pb-12 sm:pt-10 sm:pb-16 lg:pt-12 lg:pb-20 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      {/* Background Subtle Ambient Aura (Clean, non-distracting) */}
      <div className="pointer-events-none absolute top-0 left-1/2 -translate-x-1/2 w-full max-w-5xl h-96 bg-gradient-to-b from-brand-blue/10 via-transparent to-transparent blur-3xl -z-10" />

      <div className="grid lg:grid-cols-12 gap-10 lg:gap-8 items-center">
        {/* Left Column: Core Value Proposition & CTAs (6 cols on lg) */}
        <div className="lg:col-span-6 space-y-6 text-left">
          {/* Subtle Category Pill */}
          <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-surface border border-border shadow-xs text-xs font-semibold text-foreground">
            <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
            <span className="text-brand-blue font-bold">HireAI 2.0</span>
            <span className="text-border">|</span>
            <span className="text-muted-foreground flex items-center gap-1">
              <UserCheck className="w-3.5 h-3.5 text-brand-blue" />
              Intelligent Candidate Hiring & ATS
            </span>
          </div>

          {/* Clean, Human, Purpose-Driven Heading */}
          <h1 className="text-3xl sm:text-5xl lg:text-5.5xl font-extrabold font-heading text-foreground tracking-tight leading-[1.12]">
            Hire Exceptional Engineers.{' '}
            <span className="text-brand-blue">
              In Days, Not Months.
            </span>
          </h1>

          {/* Crisp, Direct Subtitle */}
          <p className="text-base sm:text-lg text-muted-foreground leading-relaxed font-sans max-w-xl">
            HireAI eliminates recruiter guesswork with automated resume parsing, transparent 0–100% neural ATS scoring, and real-time candidate skill verification.
          </p>

          {/* Action CTAs */}
          <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3.5 pt-2">
            <Link to={ROUTES.SIGNUP} className="w-full sm:w-auto">
              <Button
                size="lg"
                variant="gradient"
                className="w-full sm:w-auto font-bold px-7 py-3.5 rounded-xl text-base shadow-md hover:shadow-lg transition flex items-center justify-center gap-2"
              >
                <UserPlus className="w-4 h-4" />
                <span>Start Hiring Candidates</span>
                <ArrowRight className="w-4 h-4" />
              </Button>
            </Link>

            <Link to={ROUTES.CANDIDATE_JOBS} className="w-full sm:w-auto">
              <Button
                size="lg"
                variant="outline"
                className="w-full sm:w-auto border-border hover:bg-muted/50 font-semibold px-6 py-3.5 rounded-xl text-base transition flex items-center justify-center gap-2"
              >
                <Briefcase className="w-4 h-4 text-brand-blue" />
                <span>Browse Technical Roles</span>
              </Button>
            </Link>
          </div>

          {/* Trust Metric Micro-Grid */}
          <div className="pt-6 border-t border-border/80 grid grid-cols-3 gap-4">
            <div>
              <div className="text-xl sm:text-2xl font-extrabold font-heading text-foreground">
                96.4%
              </div>
              <div className="text-xs text-muted-foreground font-medium mt-0.5">
                ATS Match Precision
              </div>
            </div>

            <div>
              <div className="text-xl sm:text-2xl font-extrabold font-heading text-foreground">
                73%
              </div>
              <div className="text-xs text-muted-foreground font-medium mt-0.5">
                Faster Sourcing Cycle
              </div>
            </div>

            <div>
              <div className="text-xl sm:text-2xl font-extrabold font-heading text-foreground">
                45K+
              </div>
              <div className="text-xs text-muted-foreground font-medium mt-0.5">
                Vetted Candidates
              </div>
            </div>
          </div>
        </div>

        {/* Right Column: Authentic Interactive SaaS Product Preview (6 cols on lg) */}
        <div className="lg:col-span-6 relative">
          {/* Main App Preview Window */}
          <div className="rounded-2xl bg-surface border border-border shadow-xl overflow-hidden transition-all duration-300 hover:shadow-2xl">
            {/* Window macOS Top Bar */}
            <div className="px-4 py-3 bg-muted/40 border-b border-border flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="w-3 h-3 rounded-full bg-red-400/80 inline-block" />
                <span className="w-3 h-3 rounded-full bg-amber-400/80 inline-block" />
                <span className="w-3 h-3 rounded-full bg-emerald-400/80 inline-block" />
                <span className="ml-2 text-xs font-mono font-medium text-muted-foreground">
                  hireai.app / recruiter-pipeline
                </span>
              </div>
              <span className="inline-flex items-center gap-1 text-[11px] font-mono font-bold text-emerald-600 dark:text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded-md">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-ping inline-block" />
                LIVE ATS
              </span>
            </div>

            {/* In-App Live Pipeline Content */}
            <div className="p-4 sm:p-5 space-y-3.5 bg-background/50">
              {/* Filter / Search Bar */}
              <div className="flex items-center gap-2 px-3 py-2 rounded-xl bg-surface border border-border text-xs text-muted-foreground">
                <Search className="w-3.5 h-3.5 text-brand-blue" />
                <span className="truncate">Filtered by: <strong className="text-foreground">React, Java, Spring Boot, AWS</strong></span>
                <span className="ml-auto text-[10px] font-mono px-1.5 py-0.5 rounded bg-muted">14 Matches</span>
              </div>

              {/* Candidate Card 1: Top Candidate Match */}
              <div className="p-4 rounded-xl bg-surface border border-brand-blue/30 shadow-xs space-y-3 relative group">
                <div className="flex items-start justify-between gap-3">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-brand-navy to-brand-blue text-white flex items-center justify-center font-bold text-sm shadow-xs shrink-0">
                      MS
                    </div>
                    <div>
                      <div className="flex items-center gap-2">
                        <h4 className="font-bold text-sm text-foreground">Mohit Singh Chouhan</h4>
                        <span className="text-[10px] font-semibold px-1.5 py-0.2 rounded-full bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 flex items-center gap-1">
                          <BadgeCheck className="w-3 h-3" /> Verified
                        </span>
                      </div>
                      <p className="text-xs text-muted-foreground">
                        Senior Full-Stack Engineer • Ex-Vionsys
                      </p>
                    </div>
                  </div>

                  <div className="text-right shrink-0">
                    <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-brand-blue/15 text-brand-blue dark:text-brand-accent font-mono font-bold text-xs">
                      <Sparkles className="w-3 h-3" /> 96% Match
                    </span>
                  </div>
                </div>

                {/* Skills Row */}
                <div className="flex flex-wrap gap-1.5">
                  {['React', 'Java', 'Spring Boot', 'PostgreSQL', 'Docker'].map((sk) => (
                    <span
                      key={sk}
                      className="px-2 py-0.5 rounded-md text-[11px] font-medium bg-muted/60 text-foreground border border-border/50"
                    >
                      {sk}
                    </span>
                  ))}
                </div>

                {/* Recruiter Quick Action Pill */}
                <div className="pt-2 border-t border-border/60 flex items-center justify-between text-xs">
                  <span className="text-muted-foreground flex items-center gap-1 text-[11px]">
                    <Clock className="w-3 h-3 text-muted-foreground/70" /> Applied Sep 28 • Under Review
                  </span>
                  <div className="flex items-center gap-1.5">
                    <span className="px-2.5 py-1 rounded-lg bg-brand-navy text-white text-[11px] font-semibold flex items-center gap-1">
                      <UserCheck className="w-3 h-3" /> Shortlist
                    </span>
                  </div>
                </div>
              </div>

              {/* Candidate Card 2: Micro Card Preview */}
              <div className="p-3.5 rounded-xl bg-surface border border-border/70 flex items-center justify-between gap-3 text-xs opacity-90">
                <div className="flex items-center gap-3">
                  <div className="w-9 h-9 rounded-xl bg-muted text-muted-foreground font-bold flex items-center justify-center text-xs shrink-0">
                    AR
                  </div>
                  <div>
                    <div className="font-bold text-foreground">Ananya Roy</div>
                    <div className="text-[11px] text-muted-foreground">Distributed Systems • 6 yrs exp</div>
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  <span className="font-mono font-bold text-xs text-brand-blue">94% ATS</span>
                  <span className="px-2 py-0.5 rounded-md bg-muted text-muted-foreground text-[10px]">
                    Screened
                  </span>
                </div>
              </div>
            </div>

            {/* Bottom Status Ticker */}
            <div className="px-4 py-2.5 bg-muted/30 border-t border-border flex items-center justify-between text-[11px] font-mono text-muted-foreground">
              <span className="flex items-center gap-1.5">
                <Code2 className="w-3.5 h-3.5 text-brand-blue" />
                Automatic Skill Graph Extraction
              </span>
              <span className="text-emerald-600 dark:text-emerald-400 font-semibold">
                ● 1.2s Response Time
              </span>
            </div>
          </div>

          {/* Floating Live Hiring Pill (Desktop only, clean positioning) */}
          <div className="hidden sm:flex absolute -bottom-4 -left-4 bg-surface border border-border shadow-lg rounded-xl p-3 items-center gap-3 backdrop-blur-md">
            <div className="w-8 h-8 rounded-lg bg-emerald-500/15 text-emerald-600 flex items-center justify-center shrink-0">
              <CheckCircle2 className="w-4 h-4 stroke-[2.5]" />
            </div>
            <div>
              <div className="text-xs font-bold text-foreground">Offer Accepted in 4 Days</div>
              <div className="text-[10px] text-muted-foreground">Average candidate time-to-hire</div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};
