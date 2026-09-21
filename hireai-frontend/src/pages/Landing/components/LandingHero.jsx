import React from 'react';
import { Link } from 'react-router-dom';
import { Button } from '@/components/ui/Button';
import { ROUTES } from '@/constants';

export const LandingHero = () => {
  return (
    <section className="relative overflow-hidden bg-gradient-to-r from-[#6D3DF5]/10 via-[#C63FC5]/10 to-[#FC9559]/10 py-16 lg:py-24 px-6 md:px-10 rounded-3xl max-w-7xl mx-auto my-4 border border-border/40">
      <div className="mx-auto max-w-7xl">
        <div className="grid lg:grid-cols-2 gap-12 items-center">
          {/* Left Content */}
          <div className="text-left">
            {/* Badge */}
            <div className="inline-flex items-center gap-3 bg-brand-blue-light/15 text-brand-blue dark:bg-brand-blue-light/25 text-brand-blue border border-[#6D3DF5]/30 rounded-full px-5 py-2 backdrop-blur-md">
              <span className="w-3 h-3 rounded-full bg-brand-accent text-brand-dark animate-pulse" />
              <span className="text-[#6D3DF5] dark:text-[#A78BFA] font-semibold text-sm">
                All-In-One Hiring Platform
              </span>
            </div>

            {/* Heading */}
            <h1 className="mt-5 text-4xl sm:text-5xl lg:text-6xl font-extrabold font-heading leading-tight text-foreground tracking-tight">
              Simplify Hiring.
              <br />
              Empower{' '}
              <span className="gradient-text-brand">
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
                  variant="gradient"
                  className="w-full sm:w-auto bg-brand-accent text-brand-dark text-foreground font-bold px-8 py-3.5 rounded-full text-lg shadow-xl shadow-[#C63FC5]/25 hover:opacity-95 transition"
                >
                  Get Started
                </Button>
              </Link>

              <Link to={ROUTES.LOGIN}>
                <Button
                  size="lg"
                  className="w-full sm:w-auto bg-brand-navy text-white hover:bg-brand-blue font-bold px-8 py-3.5 rounded-full text-lg shadow-lg transition"
                >
                  Contact Us
                </Button>
              </Link>
            </div>
          </div>

          {/* Right Images (Dual stacked layout matching Landing_page_S Hero) */}
          <div className="relative flex justify-center lg:justify-end mt-8 lg:mt-0">
            {/* Ambient Background Glow */}
            <div className="absolute -inset-2 bg-gradient-to-r from-[#C63FC5]/20 via-[#F56681]/20 to-[#FC9559]/20 rounded-[36px] blur-2xl opacity-70 pointer-events-none" />

            {/* Primary Hero Image */}
            <img
              src="/images/banner-img.png"
              alt="HireAI Recruitment Platform"
              className="rounded-[30px] object-cover shadow-2xl border border-white/20 dark:border-white/10 w-full max-w-[540px] h-auto relative z-10"
            />

            {/* Secondary Overlapping Image Card */}
            <img
              src="/images/banner-img.png"
              alt="HireAI Analytics"
              className="hidden sm:block absolute -bottom-6 -left-6 rounded-[30px] object-cover shadow-2xl border border-white/30 dark:border-white/20 w-[240px] h-auto z-20 hover-lift"
            />
          </div>
        </div>
      </div>
    </section>
  );
};
