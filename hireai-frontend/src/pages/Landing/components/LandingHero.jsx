import React from 'react';
import { Link } from 'react-router-dom';
import { Button } from '@/components/ui/Button';
import { ROUTES } from '@/constants';

export const LandingHero = () => {
  return (
    <section className="relative overflow-hidden glass-card py-16 lg:py-24 px-6 md:px-10 max-w-7xl mx-auto my-4 border border-border/40">
      <div className="mx-auto max-w-7xl">
        <div className="grid lg:grid-cols-2 gap-12 items-center">
          {/* Left Content */}
          <div className="text-left">
            {/* Badge */}
            <div className="inline-flex items-center gap-3 glass-card px-4 py-1.5 rounded-full border border-brand-blue/30 text-xs sm:text-sm font-semibold text-brand-blue">
              <span className="w-2.5 h-2.5 rounded-full bg-brand-blue animate-pulse" />
              <span>All-In-One Hiring Platform</span>
            </div>

            {/* Heading */}
            <h1 className="mt-5 text-4xl sm:text-5xl lg:text-6xl font-extrabold font-heading leading-tight text-foreground tracking-tight">
              Simplify Hiring.
              <br />
              Empower{' '}
              <span className="text-brand-blue dark:text-brand-accent">
                Careers.
              </span>
            </h1>

            {/* Description */}
            <p className="mt-5 text-base sm:text-lg leading-relaxed text-muted-foreground font-sans max-w-xl">
              Match elite tech talent with ambitious engineering teams in real-time. 
              Eliminate manual screening friction using neural rank scoring and automated skill assessments.
            </p>

            {/* Buttons */}
            <div className="flex flex-col sm:flex-row gap-4 sm:gap-5 mt-10">
              <Link to={ROUTES.SIGNUP}>
                <Button
                  size="lg"
                  variant="primary"
                  className="w-full sm:w-auto rounded-full font-bold px-8 py-3.5 text-base shadow-lg"
                >
                  Get Started
                </Button>
              </Link>

              <Link to={ROUTES.LOGIN}>
                <Button
                  size="lg"
                  variant="outline"
                  className="w-full sm:w-auto rounded-full font-bold px-8 py-3.5 text-base"
                >
                  Sign In
                </Button>
              </Link>
            </div>
          </div>

          {/* Right Preview Section with Glass Cards */}
          <div className="relative flex justify-center lg:justify-end mt-8 lg:mt-0">
            {/* Ambient Background Glow */}
            <div className="absolute -inset-4 bg-gradient-to-r from-brand-blue/20 via-brand-navy/30 to-brand-accent/20 rounded-[36px] blur-2xl opacity-70 pointer-events-none" />

            {/* Primary Hero Container Wrapped in Glass Card */}
            <div className="glass-card p-4 sm:p-5 relative z-10 w-full max-w-[540px] shadow-2xl border border-white/20 dark:border-white/10">
              <div className="flex items-center justify-between pb-3 border-b border-border mb-3">
                <div className="flex items-center gap-2">
                  <div className="w-3 h-3 rounded-full bg-emerald-400 animate-pulse" />
                  <span className="text-xs sm:text-sm font-semibold text-foreground">
                    HireAI Recruitment Platform
                  </span>
                </div>
                <span className="text-[11px] font-semibold text-brand-blue bg-brand-blue/15 px-2.5 py-0.5 rounded-full">
                  Live Engine
                </span>
              </div>
              <img
                src="/images/banner-img.png"
                alt="HireAI Recruitment Platform"
                className="rounded-xl object-cover w-full h-auto shadow-md"
                onError={(e) => {
                  e.target.style.display = 'none';
                }}
              />
            </div>

            {/* Secondary Overlapping Stats Card wrapped in Glass Card */}
            <div className="hidden sm:flex absolute -bottom-6 -left-6 z-20 glass-card p-4 shadow-2xl items-center gap-3 border border-white/20 dark:border-white/10 animate-in fade-in zoom-in-95">
              <div className="w-10 h-10 rounded-xl bg-brand-accent/20 flex items-center justify-center text-brand-dark dark:text-brand-accent font-bold text-lg">
                AI
              </div>
              <div>
                <p className="text-xs font-bold text-foreground">HireAI Analytics</p>
                <p className="text-[11px] text-muted-foreground">99.4% Match Accuracy • Real-time</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};
